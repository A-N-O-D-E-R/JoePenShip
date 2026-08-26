package io.sablier.core;

/** Readiness based purely on the provider-reported {@link WorkloadState} — the default when no more specific check is configured. */
public final class StateReadinessChecker implements ReadinessChecker {

    @Override
    public ReadinessStatus check(Workload workload) {
        WorkloadState state = workload.state();
        return state == WorkloadState.RUNNING || state == WorkloadState.READY
                ? ReadinessStatus.ready()
                : ReadinessStatus.pending("workload state is " + state);
    }
}
