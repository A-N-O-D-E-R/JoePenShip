package io.virtualization.sdk.incus;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class IncusTlsCredentialsTest {

    @Test
    void toStringRedactsCertAndKey() {
        IncusTlsCredentials credentials = new IncusTlsCredentials("cert-pem-content", "key-pem-content");

        assertThat(credentials.toString())
                .doesNotContain("cert-pem-content")
                .doesNotContain("key-pem-content");
    }

    @Test
    void rejectsBlankOrNullFields() {
        assertThatIllegalArgumentException().isThrownBy(() -> new IncusTlsCredentials("", "key"));
        assertThatIllegalArgumentException().isThrownBy(() -> new IncusTlsCredentials("cert", " "));
        assertThatNullPointerException().isThrownBy(() -> new IncusTlsCredentials(null, "key"));
    }

    @Test
    void twoArgConstructorDefaultsToNoCustomCa() {
        IncusTlsCredentials credentials = new IncusTlsCredentials("cert", "key");

        assertThat(credentials.caCertificatePem()).isEmpty();
    }

    @Test
    void toStringRedactsCustomCaPresenceOnly() {
        IncusTlsCredentials credentials =
                new IncusTlsCredentials("cert", "key", java.util.Optional.of("ca-pem-content"));

        assertThat(credentials.toString()).doesNotContain("ca-pem-content").contains("caCertificatePem=<redacted>");
    }
}
