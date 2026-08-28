package io.virtualization.sdk.network;

import java.util.Objects;
import java.util.UUID;

public record NetworkId(String value) {

    public NetworkId {
        Objects.requireNonNull(value, "value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }

    public static NetworkId generate() {
        return new NetworkId("network-" + UUID.randomUUID());
    }
}
