package io.sablier.core.exception;

/**
 * Base type for all operation-time failures raised by Sablier: workload lookups, session
 * lookups, and operation failures.
 *
 * <p>Constructor-time precondition violations (a null or blank argument) are plain {@link
 * IllegalArgumentException} / {@link NullPointerException}, not part of this hierarchy — this
 * hierarchy is reserved for failures that occur while operating on a workload or session, not
 * while constructing an object.
 */
public class SablierException extends RuntimeException {

    public SablierException(String message) {
        super(message);
    }

    public SablierException(String message, Throwable cause) {
        super(message, cause);
    }
}
