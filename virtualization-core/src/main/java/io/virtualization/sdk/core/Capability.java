package io.virtualization.sdk.core;

/** A lifecycle operation a {@link VirtualizationProvider} may or may not support. */
public enum Capability {
    START,
    STOP,
    REBOOT,
    SHUTDOWN,
    DESTROY
}
