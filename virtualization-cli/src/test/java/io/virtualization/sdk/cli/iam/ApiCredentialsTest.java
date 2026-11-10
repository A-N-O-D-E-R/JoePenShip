package io.virtualization.sdk.cli.iam;

import io.virtualization.sdk.core.exception.AuthenticationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiCredentialsTest {

    @Test
    void resolveReturnsTheCliValueWhenPresent() {
        assertThat(ApiCredentials.resolve("vsdk_from-flag")).isEqualTo("vsdk_from-flag");
    }

    @Test
    void resolveThrowsAuthenticationExceptionWhenNeitherFlagNorEnvVarIsSet() {
        assertThatThrownBy(() -> ApiCredentials.resolve(null)).isInstanceOf(AuthenticationException.class);
        assertThatThrownBy(() -> ApiCredentials.resolve("  ")).isInstanceOf(AuthenticationException.class);
    }
}
