package io.virtualization.sdk.incus;

import java.net.URI;
import java.util.Map;
import java.util.Objects;

/**
 * A named server {@link IncusImageProvider#pull} can ask the connected Incus server to fetch an
 * image from. The Incus server itself talks to {@code server} over {@code protocol} — this SDK
 * never speaks simplestreams directly.
 */
public record IncusRemote(String name, URI server, String protocol) {

    public IncusRemote {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(server, "server must not be null");
        Objects.requireNonNull(protocol, "protocol must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }

    /** Incus's built-in default remotes, as shipped by every fresh Incus install. */
    public static Map<String, IncusRemote> defaults() {
        return Map.of(
                "images", new IncusRemote("images", URI.create("https://images.linuxcontainers.org"), "simplestreams"),
                "ubuntu", new IncusRemote("ubuntu", URI.create("https://cloud-images.ubuntu.com/releases"), "simplestreams"),
                "ubuntu-daily", new IncusRemote(
                        "ubuntu-daily", URI.create("https://cloud-images.ubuntu.com/daily"), "simplestreams"));
    }
}
