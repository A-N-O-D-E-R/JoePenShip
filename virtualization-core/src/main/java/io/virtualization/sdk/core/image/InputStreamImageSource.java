package io.virtualization.sdk.core.image;

import java.io.InputStream;
import java.util.Objects;

/**
 * Imports an image from a caller-supplied stream. The provider consumes and closes it.
 *
 * @param contentLength byte length if known, else {@code -1}
 */
public record InputStreamImageSource(InputStream stream, long contentLength) implements ImageSource {

    public InputStreamImageSource {
        Objects.requireNonNull(stream, "stream must not be null");
        if (contentLength < -1) {
            throw new IllegalArgumentException(
                    "contentLength must be -1 (unknown) or non-negative, was " + contentLength);
        }
    }
}
