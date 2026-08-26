package io.virtualization.sdk.vps;

import java.util.Objects;
import java.util.UUID;

/** The VPS layer's own identity — never assumed to equal a provider's instance name or VM id. */
public record VpsId(String value) {

    public VpsId {
        Objects.requireNonNull(value, "value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }

    public static VpsId generate() {
        return new VpsId("vps-" + UUID.randomUUID());
    }
}
