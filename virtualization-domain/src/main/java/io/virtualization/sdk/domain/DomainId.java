package io.virtualization.sdk.domain;

import java.util.Objects;
import java.util.UUID;

/** The domain layer's own identity — independent of the (normalized) domain name itself, since a domain could move between DNS providers. */
public record DomainId(String value) {

    public DomainId {
        Objects.requireNonNull(value, "value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }

    public static DomainId generate() {
        return new DomainId("domain-" + UUID.randomUUID());
    }
}
