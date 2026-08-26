package io.sablier.core.exception;

/**
 * Thrown by {@link io.sablier.core.Operation#await(java.time.Duration)} when a wait times out,
 * and by {@link io.sablier.core.SessionManager} when a workload {@code start} operation resolves
 * to {@link io.sablier.core.OperationStatus#FAILED} while creating a session.
 */
public class OperationException extends SablierException {

    public OperationException(String message) {
        super(message);
    }

    public OperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
