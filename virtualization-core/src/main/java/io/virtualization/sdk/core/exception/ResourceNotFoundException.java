package io.virtualization.sdk.core.exception;

/** Thrown when a requested resource (VM, container, node, snapshot, ...) does not exist on the provider. */
public class ResourceNotFoundException extends VirtualizationException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
