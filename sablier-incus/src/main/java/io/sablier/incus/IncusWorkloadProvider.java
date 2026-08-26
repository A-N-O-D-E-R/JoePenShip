package io.sablier.incus;

import io.sablier.core.Operation;
import io.sablier.core.ReadinessChecker;
import io.sablier.core.ReadinessCheckers;
import io.sablier.core.ReadinessStatus;
import io.sablier.core.Workload;
import io.sablier.core.WorkloadMetadata;
import io.sablier.core.WorkloadProvider;
import io.sablier.core.WorkloadState;
import io.sablier.core.exception.WorkloadNotFoundException;
import io.sablier.incus.internal.DomainMapper;
import io.sablier.incus.internal.OperationAdapter;
import io.sablier.incus.internal.SablierMetadata;
import io.virtualization.sdk.core.Container;
import io.virtualization.sdk.core.VirtualMachine;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.incus.IncusProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * {@link WorkloadProvider} backed by {@code virtualization-sdk}'s {@link IncusProvider}, scoped to
 * a single configured {@link #project}.
 *
 * <p><b>Identity</b>: a workload's {@link Workload#id()} is simply its Incus instance name — this
 * provider does not encode {@code project} into the id string, because {@link #project} is fixed
 * for the lifetime of this provider instance. A deployment that needs to activate workloads across
 * multiple Incus projects registers one {@code IncusWorkloadProvider} per project (each under its
 * own provider name, each wrapping its own {@link IncusProvider} configured for that project).
 * {@code project} and {@code location} are still preserved, structured, on every {@link Workload}
 * this provider returns — never flattened into {@link Workload#id()}.
 *
 * <p><b>Discovery</b>: only Incus instances carrying {@code user.sablier.enable=true} (and a
 * non-blank {@code user.sablier.group}) are visible through {@link #list()}/{@link
 * #findByGroup(String)}/{@link #get(String)}/{@link #readiness(String)} — see {@link
 * SablierMetadata}. {@link #start(String)}/{@link #stop(String)}/{@link #state(String)} operate on
 * any instance by id, enabled or not, mirroring the underlying {@link IncusProvider}'s own
 * lifecycle methods.
 *
 * <p>Incus instances come in two kinds — virtual machines and containers — and this provider looks
 * for a given id across both, since {@code sablier-core}'s {@link Workload} is deliberately
 * provider-neutral about which kind a workload is.
 */
public final class IncusWorkloadProvider implements WorkloadProvider {

    public static final String NAME = "incus";

    private static final Logger log = LoggerFactory.getLogger(IncusWorkloadProvider.class);

    private final IncusProvider provider;
    private final String project;
    private final Duration readinessCheckTimeout;

    public IncusWorkloadProvider(IncusProvider provider, String project, Duration readinessCheckTimeout) {
        this.provider = Objects.requireNonNull(provider, "provider must not be null");
        this.project = Objects.requireNonNull(project, "project must not be null");
        this.readinessCheckTimeout = Objects.requireNonNull(readinessCheckTimeout, "readinessCheckTimeout must not be null");
        if (project.isBlank()) {
            throw new IllegalArgumentException("project must not be blank");
        }
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Workload get(String id) {
        return requireFound(id).workload();
    }

    @Override
    public List<Workload> list() {
        List<Workload> workloads = new ArrayList<>();
        provider.listVirtualMachines().forEach(vm -> tryMap(vm).ifPresent(workloads::add));
        provider.listContainers().forEach(container -> tryMap(container).ifPresent(workloads::add));
        return List.copyOf(workloads);
    }

    @Override
    public List<Workload> findByGroup(String group) {
        return list().stream().filter(w -> w.group().equals(group)).toList();
    }

    @Override
    public Operation start(String id) {
        return dispatch(id, () -> provider.start(id));
    }

    @Override
    public Operation stop(String id) {
        return dispatch(id, () -> provider.stop(id));
    }

    @Override
    public WorkloadState state(String id) {
        return DomainMapper.toState(fetchState(id));
    }

    /**
     * Uses {@link WorkloadMetadata#readinessCheck()} (the Incus {@code user.sablier.readiness-check}
     * config key) to pick a {@link ReadinessChecker} — an HTTP/TCP check if configured, otherwise
     * falls back to {@link ReadinessCheckers#stateOnly()} (is Incus reporting the instance
     * running?). One check per call; {@code sablier-core}'s {@code ReadinessAwaiter} is what
     * polls this repeatedly with a bounded retry/timeout.
     */
    @Override
    public ReadinessStatus readiness(String id) {
        Found found = requireFound(id);
        // ponytail: builds a fresh checker (and, for HTTP, a fresh HttpClient) on every call —
        // fine at readiness-poll cadence (seconds apart), cache per-workload if this ever shows
        // up as a hot path.
        ReadinessChecker checker = found.metadata().readinessCheck()
                .<ReadinessChecker>map(spec -> ReadinessCheckers.fromSpec(spec, readinessCheckTimeout))
                .orElseGet(ReadinessCheckers::stateOnly);
        return checker.check(found.workload());
    }

    private Found requireFound(String id) {
        return find(id).orElseThrow(() -> new WorkloadNotFoundException(
                "No workload with id '" + id + "' in project '" + project + "' that is enabled for Sablier"));
    }

    private Optional<Found> find(String id) {
        try {
            VirtualMachine vm = provider.getVirtualMachine(id);
            return SablierMetadata.parse(id, vm.metadata())
                    .map(metadata -> new Found(DomainMapper.toWorkload(project, vm, metadata), metadata));
        } catch (ResourceNotFoundException notAVirtualMachine) {
            // fall through to the container lookup below
        }
        try {
            Container container = provider.getContainer(id);
            return SablierMetadata.parse(id, container.metadata())
                    .map(metadata -> new Found(DomainMapper.toWorkload(project, container, metadata), metadata));
        } catch (ResourceNotFoundException notFound) {
            return Optional.empty();
        }
    }

    private io.virtualization.sdk.core.VirtualMachineState fetchState(String id) {
        try {
            return provider.getVirtualMachine(id).state();
        } catch (ResourceNotFoundException notAVirtualMachine) {
            // fall through to the container lookup below
        }
        try {
            return provider.getContainer(id).state();
        } catch (ResourceNotFoundException notFound) {
            throw new WorkloadNotFoundException("No workload with id '" + id + "' in project '" + project + "'", notFound);
        }
    }

    private Optional<Workload> tryMap(VirtualMachine vm) {
        return SablierMetadata.parse(vm.id(), vm.metadata()).map(metadata -> DomainMapper.toWorkload(project, vm, metadata));
    }

    private Optional<Workload> tryMap(Container container) {
        return SablierMetadata.parse(container.id(), container.metadata())
                .map(metadata -> DomainMapper.toWorkload(project, container, metadata));
    }

    private Operation dispatch(String id, Supplier<io.virtualization.sdk.core.Operation> action) {
        io.virtualization.sdk.core.Operation started;
        try {
            started = action.get();
        } catch (ResourceNotFoundException e) {
            throw new WorkloadNotFoundException("No workload with id '" + id + "' in project '" + project + "'", e);
        }
        log.info("workload={} project={} operation={} dispatched", id, project, started.id());
        return new OperationAdapter(started);
    }

    private record Found(Workload workload, WorkloadMetadata metadata) {}
}
