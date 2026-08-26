package io.virtualization.sdk.core.image;

import java.nio.file.Path;
import java.util.Objects;

/** Imports an image from a local file, streamed rather than buffered in memory. */
public record LocalFileImageSource(Path path) implements ImageSource {

    public LocalFileImageSource {
        Objects.requireNonNull(path, "path must not be null");
    }
}
