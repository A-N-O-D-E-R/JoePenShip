package io.sablier.core.support;

import io.sablier.core.Operation;
import io.sablier.core.OperationHandle;
import io.sablier.core.ReadinessStatus;
import io.sablier.core.Workload;
import io.sablier.core.WorkloadProvider;
import io.sablier.core.WorkloadState;
import io.sablier.core.exception.WorkloadNotFoundException;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hand-written test double for {@link WorkloadProvider}. Tracks {@code start}/{@code stop} call
 * counts per workload id and flips the stored workload's state accordingly, so tests can assert
 * exactly how many times a lifecycle operation was actually invoked.
 */
public final class FakeWorkloadProvider implements WorkloadProvider {

    public static final String NAME = "fake";

    private final Map<String, Workload> workloads = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> startCalls = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> stopCalls = new ConcurrentHashMap<>();
    private final java.util.Set<String> neverReady = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public FakeWorkloadProvider withWorkload(Workload workload) {
        workloads.put(workload.id(), workload);
        return this;
    }

    /** Makes {@link #readiness(String)} report {@link io.sablier.core.ReadinessState#PENDING} forever for this id, regardless of state. */
    public FakeWorkloadProvider neverReady(String id) {
        neverReady.add(id);
        return this;
    }

    public int startCallCount(String id) {
        return startCalls.getOrDefault(id, new AtomicInteger()).get();
    }

    public int stopCallCount(String id) {
        return stopCalls.getOrDefault(id, new AtomicInteger()).get();
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Workload get(String id) {
        Workload workload = workloads.get(id);
        if (workload == null) {
            throw new WorkloadNotFoundException("No workload with id '" + id + "'");
        }
        return workload;
    }

    @Override
    public List<Workload> list() {
        return List.copyOf(workloads.values());
    }

    @Override
    public List<Workload> findByGroup(String group) {
        return workloads.values().stream().filter(w -> w.group().equals(group)).toList();
    }

    @Override
    public Operation start(String id) {
        startCalls.computeIfAbsent(id, ignored -> new AtomicInteger()).incrementAndGet();
        workloads.computeIfPresent(id, (ignored, w) -> withState(w, WorkloadState.RUNNING));
        return completedOperation();
    }

    @Override
    public Operation stop(String id) {
        stopCalls.computeIfAbsent(id, ignored -> new AtomicInteger()).incrementAndGet();
        workloads.computeIfPresent(id, (ignored, w) -> withState(w, WorkloadState.STOPPED));
        return completedOperation();
    }

    private static Workload withState(Workload w, WorkloadState state) {
        return new Workload(w.id(), w.name(), w.type(), state, w.group(), w.project(), w.location());
    }

    @Override
    public WorkloadState state(String id) {
        return get(id).state();
    }

    @Override
    public ReadinessStatus readiness(String id) {
        if (neverReady.contains(id)) {
            return ReadinessStatus.pending("simulated: never becomes ready");
        }
        WorkloadState state = state(id);
        return state == WorkloadState.RUNNING || state == WorkloadState.READY
                ? ReadinessStatus.ready()
                : ReadinessStatus.pending("workload is " + state);
    }

    private static Operation completedOperation() {
        OperationHandle handle = OperationHandle.create("op-" + java.util.UUID.randomUUID());
        handle.complete();
        return handle.operation();
    }
}
