package io.virtualization.sdk.certificate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class CertificateMaterialTest {

    private static final String CERT_PEM = "-----BEGIN CERTIFICATE-----\nMIIB...secret-cert...\n-----END CERTIFICATE-----";
    private static final String KEY_PEM = "-----BEGIN PRIVATE KEY-----\nMIIE...secret-key...\n-----END PRIVATE KEY-----";
    private static final String CHAIN_PEM = "-----BEGIN CERTIFICATE-----\nMIIC...secret-chain...\n-----END CERTIFICATE-----";

    @Test
    void rejectsNullCertificate() {
        assertThatNullPointerException().isThrownBy(() -> new CertificateMaterial(null, KEY_PEM, CHAIN_PEM));
    }

    @Test
    void rejectsBlankCertificate() {
        assertThatIllegalArgumentException().isThrownBy(() -> new CertificateMaterial(" ", KEY_PEM, CHAIN_PEM));
    }

    @Test
    void rejectsNullPrivateKey() {
        assertThatNullPointerException().isThrownBy(() -> new CertificateMaterial(CERT_PEM, null, CHAIN_PEM));
    }

    @Test
    void rejectsBlankPrivateKey() {
        assertThatIllegalArgumentException().isThrownBy(() -> new CertificateMaterial(CERT_PEM, " ", CHAIN_PEM));
    }

    @Test
    void rejectsNullChain() {
        assertThatNullPointerException().isThrownBy(() -> new CertificateMaterial(CERT_PEM, KEY_PEM, null));
    }

    @Test
    void rejectsBlankChain() {
        assertThatIllegalArgumentException().isThrownBy(() -> new CertificateMaterial(CERT_PEM, KEY_PEM, " "));
    }

    @Test
    void toStringRedactsAllThreePemFields() {
        CertificateMaterial material = new CertificateMaterial(CERT_PEM, KEY_PEM, CHAIN_PEM);

        String rendered = material.toString();

        assertThat(rendered).doesNotContain("secret-cert").doesNotContain("secret-key").doesNotContain("secret-chain");
        assertThat(rendered).isEqualTo("CertificateMaterial[certificate=<redacted>, privateKey=<redacted>, chain=<redacted>]");
    }
}
