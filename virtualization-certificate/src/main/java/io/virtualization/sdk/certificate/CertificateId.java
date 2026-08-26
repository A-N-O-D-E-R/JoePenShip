package io.virtualization.sdk.certificate;

import java.util.Objects;
import java.util.UUID;

public record CertificateId(String value) {

    public CertificateId {
        Objects.requireNonNull(value, "value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }

    public static CertificateId generate() {
        return new CertificateId("cert-" + UUID.randomUUID());
    }
}
