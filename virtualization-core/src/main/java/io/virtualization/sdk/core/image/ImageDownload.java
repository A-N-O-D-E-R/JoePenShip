package io.virtualization.sdk.core.image;

import io.virtualization.sdk.core.image.internal.DefaultImageDownload;

import java.io.InputStream;
import java.util.Optional;
import java.util.OptionalLong;

/**
 * The result of {@link ImageProvider#download(ImageReference)}: a streamed export of an image.
 * The caller is responsible for closing it, which closes the underlying {@link #stream()}.
 */
public interface ImageDownload extends AutoCloseable {

    InputStream stream();

    OptionalLong contentLength();

    Optional<String> mediaType();

    Optional<String> checksum();

    Optional<String> checksumAlgorithm();

    @Override
    void close();

    static ImageDownload of(
            InputStream stream, long contentLength, String mediaType, String checksum, String checksumAlgorithm) {
        return new DefaultImageDownload(stream, contentLength, mediaType, checksum, checksumAlgorithm);
    }
}
