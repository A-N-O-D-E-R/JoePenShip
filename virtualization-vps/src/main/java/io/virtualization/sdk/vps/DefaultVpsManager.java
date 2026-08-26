package io.virtualization.sdk.vps;

import io.virtualization.sdk.core.ComputeResources;
import io.virtualization.sdk.core.Operation;
import io.virtualization.sdk.core.OperationStatus;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.core.image.ImageReference;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * The only {@link VpsManager} implementation. No background threads: an in-flight {@link
 * VpsProvisioner} operation is reconciled lazily, the next time {@link #get}/{@link #list} (or
 * another lifecycle method, which calls {@code get} first) is called for that id.
 *
 * <p>Idempotency and concurrency are both handled by {@link ConcurrentHashMap#computeIfAbsent}'s
 * per-key atomicity on {@link #create} — an in-process, single-JVM guarantee, deliberately not a
 * distributed lock.
 *
 * <p>Before flipping {@code PROVISIONING}/{@code REBUILDING} to {@code READY}, a {@link
 * VpsReadinessChecker} is consulted (default: {@link VpsReadinessChecker#alwaysReady()}, i.e. no
 * check) — up to {@value #READINESS_MAX_ATTEMPTS} attempts, blocking the calling thread briefly
 * between them, still no background threads. Exhausting the attempts lands the VPS in {@code
 * ERROR} instead of {@code READY}.
 */
public final class DefaultVpsManager implements VpsManager {

    private static final ComputeResources DEFAULT_COMPUTE = new ComputeResources(1, 1_024);
    private static final StorageConfiguration DEFAULT_STORAGE = new StorageConfiguration(DataSize.ofGigabytes(10));

    private static final Set<VpsState> START_FROM = EnumSet.of(VpsState.STOPPED);
    private static final Set<VpsState> STOP_OR_SHUTDOWN_FROM = EnumSet.of(VpsState.RUNNING, VpsState.READY);
    private static final Set<VpsState> RESTART_FROM = EnumSet.of(VpsState.RUNNING, VpsState.READY);
    private static final Set<VpsState> DESTROY_FROM = EnumSet.of(VpsState.READY, VpsState.STOPPED, VpsState.RUNNING, VpsState.ERROR);
    private static final Set<VpsState> REBUILD_FROM = EnumSet.of(VpsState.READY, VpsState.STOPPED, VpsState.ERROR);

    // ponytail: fixed 3-attempt/200ms readiness retry, not configurable — make it a constructor
    // param if boot times ever vary enough to need tuning.
    private static final int READINESS_MAX_ATTEMPTS = 3;
    private static final Duration READINESS_RETRY_DELAY = Duration.ofMillis(200);

    private final VpsRepository repository;
    private final VpsProvisioner provisioner;
    private final VpsReadinessChecker readinessChecker;
    private final ConcurrentHashMap<String, CreateVpsOperation> byIdempotencyKey = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<VpsId, Operation> inFlight = new ConcurrentHashMap<>();

    public DefaultVpsManager(VpsRepository repository, VpsProvisioner provisioner) {
        this(repository, provisioner, VpsReadinessChecker.alwaysReady());
    }

    public DefaultVpsManager(VpsRepository repository, VpsProvisioner provisioner, VpsReadinessChecker readinessChecker) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.provisioner = Objects.requireNonNull(provisioner, "provisioner must not be null");
        this.readinessChecker = Objects.requireNonNull(readinessChecker, "readinessChecker must not be null");
    }

    @Override
    public CreateVpsOperation create(VpsSpec spec) {
        Objects.requireNonNull(spec, "spec must not be null");
        return spec.idempotencyKey()
                .map(key -> byIdempotencyKey.computeIfAbsent(key, k -> doCreate(spec)))
                .orElseGet(() -> doCreate(spec));
    }

    private CreateVpsOperation doCreate(VpsSpec spec) {
        VpsId id = VpsId.generate();
        Instant now = Instant.now();
        Vps vps = new Vps(
                id, spec.name(), VpsState.PROVISIONING, spec.type(), spec.image(),
                spec.compute().orElse(DEFAULT_COMPUTE), spec.storage().orElse(DEFAULT_STORAGE),
                spec.network().orElse(NetworkConfiguration.UNSPECIFIED), spec,
                null, spec.project().orElse(null), null, now, now, null, null, null);
        repository.save(vps);
        CreateVpsOperation operation = provisioner.create(id, spec);
        inFlight.put(id, operation);
        return operation;
    }

    @Override
    public Vps get(VpsId id) {
        Objects.requireNonNull(id, "id must not be null");
        reconcile(id);
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("No VPS with id '" + id.value() + "'"));
    }

    @Override
    public List<Vps> list() {
        inFlight.keySet().forEach(this::reconcile);
        return repository.findAll();
    }

    @Override
    public Operation start(VpsId id) {
        return transition(id, START_FROM, VpsState.STARTING, provisioner::start);
    }

    @Override
    public Operation stop(VpsId id) {
        return transition(id, STOP_OR_SHUTDOWN_FROM, VpsState.STOPPING, provisioner::stop);
    }

    @Override
    public Operation restart(VpsId id) {
        return transition(id, RESTART_FROM, VpsState.STARTING, provisioner::restart);
    }

    @Override
    public Operation shutdown(VpsId id) {
        return transition(id, STOP_OR_SHUTDOWN_FROM, VpsState.STOPPING, provisioner::shutdown);
    }

    @Override
    public Operation destroy(VpsId id) {
        return transition(id, DESTROY_FROM, VpsState.DESTROYING, provisioner::destroy);
    }

    @Override
    public CreateVpsOperation rebuild(VpsId id, ImageReference image) {
        Objects.requireNonNull(image, "image must not be null");
        Vps current = requireTransitionable(id, REBUILD_FROM, VpsState.REBUILDING);
        Vps transient_ = withState(current, VpsState.REBUILDING, Instant.now());
        repository.save(transient_);
        CreateVpsOperation operation = provisioner.rebuild(id, transient_, image);
        inFlight.put(id, operation);
        return operation;
    }

    private Operation transition(VpsId id, Set<VpsState> legalFrom, VpsState transientState, BiFunction<VpsId, Vps, Operation> action) {
        Vps current = requireTransitionable(id, legalFrom, transientState);
        Vps updated = withState(current, transientState, Instant.now());
        repository.save(updated);
        Operation operation = action.apply(id, updated);
        inFlight.put(id, operation);
        return operation;
    }

    private Vps requireTransitionable(VpsId id, Set<VpsState> legalFrom, VpsState target) {
        Vps current = get(id);
        if (!legalFrom.contains(current.state()) || !current.state().canTransitionTo(target)) {
            throw new InvalidVpsStateException("Cannot transition VPS '" + id.value() + "' from " + current.state() + " to " + target);
        }
        return current;
    }

    private void reconcile(VpsId id) {
        inFlight.computeIfPresent(id, (key, operation) -> {
            if (operation.status() == OperationStatus.RUNNING) {
                return operation;
            }
            Vps current = repository.findById(id).orElse(null);
            if (current == null) {
                return null;
            }
            if (operation.status() == OperationStatus.FAILED) {
                repository.save(withState(current, VpsState.ERROR, Instant.now()));
                return null;
            }
            Vps merged = current;
            if (operation instanceof CreateVpsOperation createOperation) {
                merged = mergeProvisioned(current, createOperation.vps().orElse(null));
            }
            VpsState terminal = terminalStateFor(current.state());
            if (terminal == VpsState.READY && !waitForReadiness(merged)) {
                terminal = VpsState.ERROR;
            }
            repository.save(withState(merged, terminal, Instant.now()));
            return null;
        });
    }

    private boolean waitForReadiness(Vps vps) {
        for (int attempt = 1; attempt <= READINESS_MAX_ATTEMPTS; attempt++) {
            if (readinessChecker.isReady(vps)) {
                return true;
            }
            if (attempt < READINESS_MAX_ATTEMPTS) {
                sleepUninterruptibly(READINESS_RETRY_DELAY);
            }
        }
        return false;
    }

    private static void sleepUninterruptibly(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static VpsState terminalStateFor(VpsState transientState) {
        return switch (transientState) {
            case PROVISIONING, REBUILDING -> VpsState.READY;
            case STARTING -> VpsState.RUNNING;
            case STOPPING -> VpsState.STOPPED;
            case DESTROYING -> VpsState.DESTROYED;
            default -> throw new IllegalStateException("VPS not in a transient state: " + transientState);
        };
    }

    private static Vps withState(Vps vps, VpsState newState, Instant now) {
        Instant startedAt = newState == VpsState.RUNNING ? now : vps.startedAt();
        Instant stoppedAt = newState == VpsState.STOPPED ? now : vps.stoppedAt();
        Instant destroyedAt = newState == VpsState.DESTROYED ? now : vps.destroyedAt();
        return new Vps(
                vps.id(), vps.name(), newState, vps.type(), vps.image(), vps.compute(), vps.storage(), vps.network(),
                vps.spec(), vps.provider(), vps.project(), vps.workloadId(), vps.createdAt(), now, startedAt, stoppedAt,
                destroyedAt);
    }

    private static Vps mergeProvisioned(Vps vps, Vps provisioned) {
        if (provisioned == null) {
            return vps;
        }
        return new Vps(
                vps.id(), vps.name(), vps.state(), vps.type(), provisioned.image(), vps.compute(), vps.storage(),
                vps.network(), vps.spec(), provisioned.provider(), provisioned.project(), provisioned.workloadId(),
                vps.createdAt(), vps.updatedAt(), vps.startedAt(), vps.stoppedAt(), vps.destroyedAt());
    }
}
