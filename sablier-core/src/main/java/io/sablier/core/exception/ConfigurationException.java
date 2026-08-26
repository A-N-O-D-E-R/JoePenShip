package io.sablier.core.exception;

/** Thrown when provider configuration is invalid (missing field, malformed TLS material, unknown type). */
public class ConfigurationException extends SablierException {

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
