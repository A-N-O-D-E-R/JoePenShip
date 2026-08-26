package io.virtualization.sdk.vps;

/**
 * The kind of workload a VPS is backed by. Deliberately distinct from {@code
 * io.virtualization.sdk.core.image.WorkloadType} (same two values) — mapped explicitly at the
 * orchestration boundary rather than reused directly, keeping the VPS vocabulary decoupled.
 */
public enum VpsType {
    CONTAINER,
    VIRTUAL_MACHINE
}
