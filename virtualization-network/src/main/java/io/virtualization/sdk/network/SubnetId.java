package io.virtualization.sdk.network;

import java.util.Objects;
import java.util.UUID;

public record SubnetId(String value) {

    public SubnetId {
        Objects.requireNonNull(value, "value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }

    public static SubnetId generate() {
        return new SubnetId("subnet-" + UUID.randomUUID());
    }
}
