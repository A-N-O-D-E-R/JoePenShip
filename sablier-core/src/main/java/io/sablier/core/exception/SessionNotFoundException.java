package io.sablier.core.exception;

/** Thrown when a requested session id does not exist. */
public class SessionNotFoundException extends SablierException {

    public SessionNotFoundException(String message) {
        super(message);
    }

    public SessionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
