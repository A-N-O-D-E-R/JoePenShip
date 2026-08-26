package io.virtualization.sdk.core;

/** Observed power state of a {@link VirtualMachine} or {@link Container}. */
public enum VirtualMachineState {
    RUNNING,
    STOPPED,
    PAUSED,
    SUSPENDED,
    UNKNOWN
}
