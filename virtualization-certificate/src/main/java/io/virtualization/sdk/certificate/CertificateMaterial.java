package io.virtualization.sdk.certificate;

import java.util.Objects;

/**
 * The sensitive counterpart to {@link Certificate} — held behind {@link CertificateStore}, never
 * returned from {@link CertificateManager} or any {@code Operation} type. See {@link
 * CertificateStore}'s javadoc for the full "never log/never expose" contract this exists to
 * protect.
 *
 * @param certificate PEM-encoded leaf certificate
 * @param privateKey  PEM-encoded private key — the actual secret
 * @param chain       PEM-encoded intermediate certificate chain
 */
public record CertificateMaterial(String certificate, String privateKey, String chain) {

    public CertificateMaterial {
        Objects.requireNonNull(certificate, "certificate must not be null");
        Objects.requireNonNull(privateKey, "privateKey must not be null");
        Objects.requireNonNull(chain, "chain must not be null");
        if (certificate.isBlank()) {
            throw new IllegalArgumentException("certificate must not be blank");
        }
        if (privateKey.isBlank()) {
            throw new IllegalArgumentException("privateKey must not be blank");
        }
        if (chain.isBlank()) {
            throw new IllegalArgumentException("chain must not be blank");
        }
    }

    /** Fully redacted — none of the three PEM fields are safe to log, unlike e.g. a credential's non-secret id field. */
    @Override
    public String toString() {
        return "CertificateMaterial[certificate=<redacted>, privateKey=<redacted>, chain=<redacted>]";
    }
}
