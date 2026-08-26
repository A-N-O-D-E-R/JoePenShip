package io.sablier.core;

/** Observed lifecycle state of a {@link Workload}, as reported by its {@link WorkloadProvider}. */
public enum WorkloadState {
    UNKNOWN,
    STOPPED,
    STARTING,
    RUNNING,
    READY,
    STOPPING,
    ERROR
}
