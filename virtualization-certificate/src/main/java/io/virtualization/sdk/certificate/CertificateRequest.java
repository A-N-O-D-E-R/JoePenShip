package io.virtualization.sdk.certificate;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * What a caller supplies to {@link CertificateManager#requestCertificate}. {@code domains} are
 * plain strings, not {@code DomainId}s — this module has no dependency on {@code
 * virtualization-domain}; a caller that wants normalized names (see {@code DomainNames.normalize}
 * there) normalizes them itself before building this request.
 */
public final class CertificateRequest {

    private final List<String> domains;
    private final String issuer;
    private final ChallengeType challenge;

    private CertificateRequest(Builder builder) {
        if (builder.domains.isEmpty()) {
            throw new IllegalArgumentException("domains must not be empty");
        }
        for (String domain : builder.domains) {
            if (domain == null || domain.isBlank()) {
                throw new IllegalArgumentException("domains must not contain a null or blank entry");
            }
        }
        this.domains = List.copyOf(builder.domains);
        this.issuer = Objects.requireNonNull(builder.issuer, "issuer must not be null");
        if (issuer.isBlank()) {
            throw new IllegalArgumentException("issuer must not be blank");
        }
        this.challenge = builder.challenge;
    }

    public List<String> domains() {
        return domains;
    }

    public String issuer() {
        return issuer;
    }

    public ChallengeType challenge() {
        return challenge;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private List<String> domains = List.of();
        private String issuer;
        private ChallengeType challenge = ChallengeType.DNS_01;

        private Builder() {}

        public Builder domains(String... domains) {
            this.domains = Arrays.asList(domains);
            return this;
        }

        public Builder domains(List<String> domains) {
            this.domains = domains;
            return this;
        }

        public Builder issuer(String issuer) {
            this.issuer = issuer;
            return this;
        }

        public Builder challenge(ChallengeType challenge) {
            this.challenge = challenge;
            return this;
        }

        public CertificateRequest build() {
            return new CertificateRequest(this);
        }
    }
}
