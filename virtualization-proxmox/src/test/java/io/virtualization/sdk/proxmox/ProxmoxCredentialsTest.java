package io.virtualization.sdk.proxmox;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class ProxmoxCredentialsTest {

    @Test
    void toStringRedactsSecret() {
        ProxmoxCredentials credentials = new ProxmoxCredentials("root@pam!sdk", "super-secret-value");

        assertThat(credentials.toString())
                .contains("root@pam!sdk")
                .doesNotContain("super-secret-value");
    }

    @Test
    void authorizationHeaderContainsBothParts() {
        ProxmoxCredentials credentials = new ProxmoxCredentials("root@pam!sdk", "super-secret-value");

        assertThat(credentials.toAuthorizationHeaderValue())
                .isEqualTo("PVEAPIToken=root@pam!sdk=super-secret-value");
    }

    @Test
    void rejectsBlankOrNullFields() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ProxmoxCredentials("", "secret"));
        assertThatIllegalArgumentException().isThrownBy(() -> new ProxmoxCredentials("token", " "));
        assertThatNullPointerException().isThrownBy(() -> new ProxmoxCredentials(null, "secret"));
    }
}
