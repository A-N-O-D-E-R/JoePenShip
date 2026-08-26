package io.virtualization.sdk.certificate;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Certificate metadata only — never a private key, cert body, or chain (see {@link
 * CertificateMaterial} for that, held behind {@link CertificateStore}, never here).
 *
 * @param issuer     the {@link AcmeProviderRegistry} key that issued (or will issue) this
 *                    certificate — echoed from {@link CertificateRequest#issuer()}, the same role
 *                    {@code Domain.dnsProvider} plays for DNS providers
 * @param issuedAt   {@code null} until actually issued (a {@code REQUESTED}/{@code PENDING}
 *                    certificate has neither this nor {@code expiresAt} yet)
 */
public record Certificate(CertificateId id, CertificateStatus status, List<String> domains, Instant issuedAt, Instant expiresAt, String issuer) {

    public Certificate {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(domains, "domains must not be null");
        Objects.requireNonNull(issuer, "issuer must not be null");
        if (issuer.isBlank()) {
            throw new IllegalArgumentException("issuer must not be blank");
        }
        if (domains.isEmpty()) {
            throw new IllegalArgumentException("domains must not be empty");
        }
        for (String domain : domains) {
            if (domain == null || domain.isBlank()) {
                throw new IllegalArgumentException("domains must not contain a null or blank entry");
            }
        }
        domains = List.copyOf(domains);
    }
}
