package io.virtualization.sdk.incus;

import java.util.Objects;
import java.util.Optional;

/**
 * Client certificate credentials for Incus's mutual-TLS authentication — Incus has no bearer
 * token header; the trusted client certificate presented during the TLS handshake identifies the
 * caller.
 *
 * <p>Server trust is, in order of precedence: {@link IncusClientConfig#verifySsl()} {@code ==
 * false} (trust any server certificate — an explicit, development-only opt-in); otherwise {@link
 * #caCertificatePem()} if present (trust only that CA); otherwise the JVM's default trust store
 * (a real, publicly- or org-CA-signed Incus endpoint).
 *
 * @param clientCertificatePem PEM-encoded X.509 client certificate ({@code -----BEGIN CERTIFICATE-----})
 * @param clientKeyPem         PEM-encoded PKCS#8 private key ({@code -----BEGIN PRIVATE KEY-----}).
 *                             Legacy PKCS#1 ({@code -----BEGIN RSA PRIVATE KEY-----}) is not
 *                             supported without a third-party crypto library — convert with
 *                             {@code openssl pkcs8 -topk8 -nocrypt} if needed.
 * @param caCertificatePem     PEM-encoded CA certificate to validate the server against, instead
 *                             of the JVM's default trust store — for a private/internal CA.
 */
public record IncusTlsCredentials(String clientCertificatePem, String clientKeyPem, Optional<String> caCertificatePem) {

    public IncusTlsCredentials {
        Objects.requireNonNull(clientCertificatePem, "clientCertificatePem must not be null");
        Objects.requireNonNull(clientKeyPem, "clientKeyPem must not be null");
        Objects.requireNonNull(caCertificatePem, "caCertificatePem must not be null (use Optional.empty())");
        if (clientCertificatePem.isBlank()) {
            throw new IllegalArgumentException("clientCertificatePem must not be blank");
        }
        if (clientKeyPem.isBlank()) {
            throw new IllegalArgumentException("clientKeyPem must not be blank");
        }
    }

    /** Client certificate auth, no custom CA (JVM default trust store, unless {@code verifySsl} disables it). */
    public IncusTlsCredentials(String clientCertificatePem, String clientKeyPem) {
        this(clientCertificatePem, clientKeyPem, Optional.empty());
    }

    @Override
    public String toString() {
        return "IncusTlsCredentials[clientCertificatePem=<redacted>, clientKeyPem=<redacted>, "
                + "caCertificatePem=" + (caCertificatePem.isPresent() ? "<redacted>" : "empty") + "]";
    }
}
