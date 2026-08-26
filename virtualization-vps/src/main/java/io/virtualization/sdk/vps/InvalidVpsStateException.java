package io.virtualization.sdk.vps;

import io.virtualization.sdk.core.exception.VirtualizationException;

/** Thrown when a lifecycle operation is requested from a {@link VpsState} it isn't legal from. */
public class InvalidVpsStateException extends VirtualizationException {

    public InvalidVpsStateException(String message) {
        super(message);
    }
}
