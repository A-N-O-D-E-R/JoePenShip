package io.virtualization.sdk.core.image;

import io.virtualization.sdk.core.exception.UnsupportedCapabilityException;
import io.virtualization.sdk.core.image.support.FakeImageProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class ImageProviderTest {

    private final ImageReference reference = new ImageReference(FakeImageProvider.NAME, "images", "ubuntu/24.04");

    @Test
    void listReturnsImagesWhenCapabilitySupported() {
        ImageProvider provider = new FakeImageProvider(ImageCapabilities.of(ImageCapability.LIST));

        assertThat(provider.list()).hasSize(1);
        assertThat(provider.list().getFirst().name()).isEqualTo("ubuntu/24.04");
    }

    @Test
    void getResolvesKnownReferenceAndReturnsEmptyForUnknown() {
        ImageProvider provider = new FakeImageProvider(ImageCapabilities.of(ImageCapability.INSPECT));

        assertThat(provider.get(reference)).isPresent();
        assertThat(provider.get(new ImageReference(FakeImageProvider.NAME, "images", "debian/13"))).isEmpty();
    }

    @Test
    void searchFiltersByQuery() {
        ImageProvider provider = new FakeImageProvider(ImageCapabilities.of(ImageCapability.SEARCH));

        assertThat(provider.search(ImageQuery.builder().distribution("ubuntu").build())).hasSize(1);
        assertThat(provider.search(ImageQuery.builder().distribution("debian").build())).isEmpty();
    }

    @Test
    void unsupportedCapabilityThrows() {
        ImageProvider provider = new FakeImageProvider(ImageCapabilities.of());

        assertThatExceptionOfType(UnsupportedCapabilityException.class).isThrownBy(provider::list);
    }

    @Test
    void pullDownloadAndImportDefaultToUnsupported() {
        ImageProvider provider = new FakeImageProvider(ImageCapabilities.of(ImageCapability.LIST));

        assertThatExceptionOfType(UnsupportedCapabilityException.class).isThrownBy(() -> provider.pull(reference));
        assertThatExceptionOfType(UnsupportedCapabilityException.class).isThrownBy(() -> provider.download(reference));
        assertThatExceptionOfType(UnsupportedCapabilityException.class)
                .isThrownBy(() -> provider.importImage(new LocalFileImageSource(java.nio.file.Path.of("x.tar.gz"))));
    }
}
