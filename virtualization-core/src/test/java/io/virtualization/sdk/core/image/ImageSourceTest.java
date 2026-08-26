package io.virtualization.sdk.core.image;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class ImageSourceTest {

    @Test
    void localFileImageSourceRejectsNullPath() {
        LocalFileImageSource source = new LocalFileImageSource(Path.of("ubuntu.tar.gz"));
        assertThat(source.path()).isEqualTo(Path.of("ubuntu.tar.gz"));

        assertThatNullPointerException().isThrownBy(() -> new LocalFileImageSource(null));
    }

    @Test
    void inputStreamImageSourceAllowsUnknownLengthButRejectsInvalid() {
        InputStreamImageSource unknown = new InputStreamImageSource(new ByteArrayInputStream(new byte[0]), -1);
        assertThat(unknown.contentLength()).isEqualTo(-1);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> new InputStreamImageSource(new ByteArrayInputStream(new byte[0]), -2));
        assertThatNullPointerException().isThrownBy(() -> new InputStreamImageSource(null, 0));
    }

    @Test
    void urlImageSourceRejectsNullUrl() {
        URLImageSource source = new URLImageSource(URI.create("https://example.com/ubuntu.tar.gz"));
        assertThat(source.url()).hasHost("example.com");

        assertThatNullPointerException().isThrownBy(() -> new URLImageSource(null));
    }

    @Test
    void remoteImageSourceRejectsNullReference() {
        ImageReference reference = new ImageReference("incus", "images", "ubuntu/24.04");
        RemoteImageSource source = new RemoteImageSource(reference);
        assertThat(source.reference()).isEqualTo(reference);

        assertThatNullPointerException().isThrownBy(() -> new RemoteImageSource(null));
    }
}
