package io.virtualization.sdk.core.exception;

/** Thrown when a provider rejects the configured credentials. */
public class AuthenticationException extends VirtualizationException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
