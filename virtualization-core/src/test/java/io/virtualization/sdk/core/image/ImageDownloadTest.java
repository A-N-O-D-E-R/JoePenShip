package io.virtualization.sdk.core.image;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

class ImageDownloadTest {

    @Test
    void exposesMetadataAndClosesUnderlyingStream() throws Exception {
        boolean[] closed = {false};
        InputStream raw = new ByteArrayInputStream(new byte[] {1, 2, 3}) {
            @Override
            public void close() {
                closed[0] = true;
            }
        };

        ImageDownload download = ImageDownload.of(raw, 3, "application/x-tar", "abc123", "sha256");

        assertThat(download.contentLength()).hasValue(3);
        assertThat(download.mediaType()).contains("application/x-tar");
        assertThat(download.checksum()).contains("abc123");
        assertThat(download.checksumAlgorithm()).contains("sha256");

        download.close();
        assertThat(closed[0]).isTrue();
    }

    @Test
    void unknownContentLengthIsEmpty() {
        ImageDownload download = ImageDownload.of(new ByteArrayInputStream(new byte[0]), -1, null, null, null);

        assertThat(download.contentLength()).isEmpty();
        assertThat(download.mediaType()).isEmpty();
        download.close();
    }
}
