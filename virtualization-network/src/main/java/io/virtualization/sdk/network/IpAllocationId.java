package io.virtualization.sdk.network;

import java.util.Objects;
import java.util.UUID;

public record IpAllocationId(String value) {

    public IpAllocationId {
        Objects.requireNonNull(value, "value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }

    public static IpAllocationId generate() {
        return new IpAllocationId("ipalloc-" + UUID.randomUUID());
    }
}
