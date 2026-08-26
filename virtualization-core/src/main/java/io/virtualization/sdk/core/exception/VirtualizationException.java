package io.virtualization.sdk.core.exception;

/**
 * Base type for all provider-operation-time failures raised by the SDK.
 *
 * <p>This hierarchy is reserved for failures that occur while talking to a provider backend
 * (authentication, connectivity, remote resource lookup, unsupported capability, misconfigured
 * wiring). Precondition violations on object construction (a null or blank argument) are plain
 * {@link IllegalArgumentException} / {@link NullPointerException}, not part of this hierarchy.
 */
public class VirtualizationException extends RuntimeException {

    public VirtualizationException(String message) {
        super(message);
    }

    public VirtualizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
