package io.virtualization.sdk.core.image;

import java.net.URI;
import java.util.Objects;

/**
 * Imports an image by fetching it from an arbitrary URL.
 *
 * <p>Security-sensitive: fetching a caller- or attacker-influenced URL server-side is a classic
 * SSRF vector, so providers must not enable this by default — support is opt-in per provider
 * instance.
 */
public record URLImageSource(URI url) implements ImageSource {

    public URLImageSource {
        Objects.requireNonNull(url, "url must not be null");
    }
}
