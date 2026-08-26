package io.sablier.core;

/** Terminal or in-flight state of an {@link Operation}. */
public enum OperationStatus {
    RUNNING,
    SUCCEEDED,
    FAILED
}
