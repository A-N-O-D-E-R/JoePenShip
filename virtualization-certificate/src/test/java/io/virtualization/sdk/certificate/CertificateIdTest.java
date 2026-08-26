package io.virtualization.sdk.certificate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class CertificateIdTest {

    @Test
    void generateProducesUniqueNonBlankValues() {
        CertificateId a = CertificateId.generate();
        CertificateId b = CertificateId.generate();

        assertThat(a.value()).isNotBlank();
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void rejectsNullOrBlank() {
        assertThatNullPointerException().isThrownBy(() -> new CertificateId(null));
        assertThatIllegalArgumentException().isThrownBy(() -> new CertificateId(""));
    }
}
