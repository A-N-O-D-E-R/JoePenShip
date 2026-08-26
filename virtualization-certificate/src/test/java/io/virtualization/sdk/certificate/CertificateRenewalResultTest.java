package io.virtualization.sdk.certificate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class CertificateRenewalResultTest {

    private static final CertificateId ID = CertificateId.generate();

    @Test
    void rejectsSuccessWithAnErrorMessage() {
        assertThatIllegalArgumentException().isThrownBy(() -> new CertificateRenewalResult(ID, true, "should not happen"));
    }

    @Test
    void rejectsFailureWithoutAnErrorMessage() {
        assertThatIllegalArgumentException().isThrownBy(() -> new CertificateRenewalResult(ID, false, null));
    }
}
