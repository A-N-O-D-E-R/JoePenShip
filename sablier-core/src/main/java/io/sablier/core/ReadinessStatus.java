package io.sablier.core;

import java.util.Objects;

/** The outcome of a readiness check, as returned by {@link WorkloadProvider#readiness(String)}. */
public record ReadinessStatus(ReadinessState state, String message) {

    public ReadinessStatus {
        Objects.requireNonNull(state, "state must not be null");
        message = message != null ? message : "";
    }

    public static ReadinessStatus ready() {
        return new ReadinessStatus(ReadinessState.READY, "");
    }

    public static ReadinessStatus pending(String message) {
        return new ReadinessStatus(ReadinessState.PENDING, message);
    }

    public static ReadinessStatus failed(String message) {
        return new ReadinessStatus(ReadinessState.FAILED, message);
    }
}
