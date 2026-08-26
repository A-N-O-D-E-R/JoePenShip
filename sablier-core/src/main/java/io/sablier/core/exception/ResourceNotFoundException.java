package io.sablier.core.exception;

/** Thrown when a requested resource (instance, operation, ...) does not exist on the provider backend. */
public class ResourceNotFoundException extends SablierException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
