package io.virtualization.sdk.core.exception;

/** Thrown when SDK wiring is invalid: an unknown provider name, or invalid provider configuration. */
public class ConfigurationException extends VirtualizationException {

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
