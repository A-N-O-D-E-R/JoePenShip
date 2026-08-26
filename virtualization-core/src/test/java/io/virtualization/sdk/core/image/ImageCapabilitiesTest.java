package io.virtualization.sdk.core.image;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ImageCapabilitiesTest {

    @Test
    void supportsReturnsTrueOnlyForDeclaredCapabilities() {
        ImageCapabilities capabilities = ImageCapabilities.of(ImageCapability.LIST, ImageCapability.SEARCH);

        assertThat(capabilities.supports(ImageCapability.LIST)).isTrue();
        assertThat(capabilities.supports(ImageCapability.SEARCH)).isTrue();
        assertThat(capabilities.supports(ImageCapability.DELETE)).isFalse();
    }

    @Test
    void emptyByDefault() {
        ImageCapabilities capabilities = ImageCapabilities.of();

        assertThat(capabilities.all()).isEmpty();
    }

    @Test
    void allIsImmutable() {
        ImageCapabilities capabilities = ImageCapabilities.of(ImageCapability.LIST);

        assertThat(capabilities.all()).isUnmodifiable();
    }
}
