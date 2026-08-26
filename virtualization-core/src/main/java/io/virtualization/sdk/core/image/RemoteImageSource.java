package io.virtualization.sdk.core.image;

import java.util.Objects;

/** Imports an image sourced from another provider or remote's existing image. */
public record RemoteImageSource(ImageReference reference) implements ImageSource {

    public RemoteImageSource {
        Objects.requireNonNull(reference, "reference must not be null");
    }
}
