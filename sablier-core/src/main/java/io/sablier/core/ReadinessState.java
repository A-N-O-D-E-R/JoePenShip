package io.sablier.core;

/** Outcome of a single {@code ReadinessChecker} check. */
public enum ReadinessState {
    PENDING,
    READY,
    FAILED
}
