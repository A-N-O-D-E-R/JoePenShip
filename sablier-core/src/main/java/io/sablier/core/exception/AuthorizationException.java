package io.sablier.core.exception;

/** Thrown when authenticated credentials lack permission for the requested operation. */
public class AuthorizationException extends SablierException {

    public AuthorizationException(String message) {
        super(message);
    }

    public AuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
