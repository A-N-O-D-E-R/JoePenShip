package io.sablier.core.exception;

/** Thrown when a provider backend cannot be reached (network failure, timeout, TLS failure). */
public class ConnectionException extends SablierException {

    public ConnectionException(String message) {
        super(message);
    }

    public ConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
