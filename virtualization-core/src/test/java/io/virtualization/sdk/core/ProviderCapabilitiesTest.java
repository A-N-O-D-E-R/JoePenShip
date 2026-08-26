package io.virtualization.sdk.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProviderCapabilitiesTest {

    @Test
    void supportsReturnsTrueOnlyForDeclaredCapabilities() {
        ProviderCapabilities capabilities = ProviderCapabilities.of(Capability.START, Capability.STOP);

        assertThat(capabilities.supports(Capability.START)).isTrue();
        assertThat(capabilities.supports(Capability.STOP)).isTrue();
        assertThat(capabilities.supports(Capability.DESTROY)).isFalse();
    }

    @Test
    void emptyByDefault() {
        ProviderCapabilities capabilities = ProviderCapabilities.of();

        assertThat(capabilities.all()).isEmpty();
    }

    @Test
    void allIsImmutable() {
        ProviderCapabilities capabilities = ProviderCapabilities.of(Capability.START);

        assertThat(capabilities.all()).isUnmodifiable();
    }
}
