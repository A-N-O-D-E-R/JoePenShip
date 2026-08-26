package io.virtualization.sdk.core.exception;

/** Thrown when an asynchronous operation fails, or times out while awaiting completion. */
public class OperationException extends VirtualizationException {

    public OperationException(String message) {
        super(message);
    }

    public OperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
