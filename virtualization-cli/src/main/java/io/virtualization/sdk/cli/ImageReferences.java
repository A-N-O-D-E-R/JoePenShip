package io.virtualization.sdk.cli;

import io.virtualization.sdk.core.image.ImageReference;

/** Parses a CLI-supplied {@code remote:identifier} (or bare {@code identifier}) string into an {@link ImageReference}. */
final class ImageReferences {

    private ImageReferences() {}

    static ImageReference parse(String providerName, String text) {
        int colon = text.indexOf(':');
        if (colon < 0) {
            return new ImageReference(providerName, text);
        }
        return new ImageReference(providerName, text.substring(0, colon), text.substring(colon + 1));
    }
}
