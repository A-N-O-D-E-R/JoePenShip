package io.virtualization.sdk.iam;

import java.util.Objects;
import java.util.UUID;

public record ApiCredentialId(String value) {

    public ApiCredentialId {
        Objects.requireNonNull(value, "value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }

    public static ApiCredentialId generate() {
        return new ApiCredentialId("apicred-" + UUID.randomUUID());
    }
}
