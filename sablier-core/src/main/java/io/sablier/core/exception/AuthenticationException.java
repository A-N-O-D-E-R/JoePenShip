package io.sablier.core.exception;

/** Thrown when a provider rejects the configured credentials. */
public class AuthenticationException extends SablierException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
