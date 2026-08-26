/**
 * Provider-neutral certificate metadata and {@link io.virtualization.sdk.certificate.AcmeProvider}
 * coordination: {@link io.virtualization.sdk.certificate.CertificateManager} requests, renews and
 * revokes {@link io.virtualization.sdk.certificate.Certificate}s without any ACME protocol logic
 * of its own. No Let's Encrypt/ZeroSSL dependency, no DNS-01 challenge orchestration (that needs
 * {@code virtualization-dns} — a later phase), no deployment, no CLI/REST/Spring here yet. Pure
 * Java, no framework dependencies.
 */
package io.virtualization.sdk.certificate;
