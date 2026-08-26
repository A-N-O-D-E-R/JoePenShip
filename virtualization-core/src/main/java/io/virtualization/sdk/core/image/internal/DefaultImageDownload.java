package io.virtualization.sdk.core.image.internal;

import io.virtualization.sdk.core.image.ImageDownload;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

/** Not part of the public API — obtain instances via {@link ImageDownload#of}. */
public final class DefaultImageDownload implements ImageDownload {

    private final InputStream stream;
    private final long contentLength;
    private final String mediaType;
    private final String checksum;
    private final String checksumAlgorithm;

    public DefaultImageDownload(
            InputStream stream, long contentLength, String mediaType, String checksum, String checksumAlgorithm) {
        this.stream = Objects.requireNonNull(stream, "stream must not be null");
        this.contentLength = contentLength;
        this.mediaType = mediaType;
        this.checksum = checksum;
        this.checksumAlgorithm = checksumAlgorithm;
    }

    @Override
    public InputStream stream() {
        return stream;
    }

    @Override
    public OptionalLong contentLength() {
        return contentLength < 0 ? OptionalLong.empty() : OptionalLong.of(contentLength);
    }

    @Override
    public Optional<String> mediaType() {
        return Optional.ofNullable(mediaType);
    }

    @Override
    public Optional<String> checksum() {
        return Optional.ofNullable(checksum);
    }

    @Override
    public Optional<String> checksumAlgorithm() {
        return Optional.ofNullable(checksumAlgorithm);
    }

    @Override
    public void close() {
        try {
            stream.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
