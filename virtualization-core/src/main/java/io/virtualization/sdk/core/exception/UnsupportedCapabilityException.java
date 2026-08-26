package io.virtualization.sdk.core.exception;

/** Thrown when an operation is invoked against a provider that does not support the required capability. */
public class UnsupportedCapabilityException extends VirtualizationException {

    public UnsupportedCapabilityException(String message) {
        super(message);
    }

    public UnsupportedCapabilityException(String message, Throwable cause) {
        super(message, cause);
    }
}
