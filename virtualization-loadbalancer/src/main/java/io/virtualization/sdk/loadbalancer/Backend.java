package io.virtualization.sdk.loadbalancer;

import java.util.Objects;

/** One reverse-proxy upstream target. */
public record Backend(String host, int port) {

    public Backend {
        Objects.requireNonNull(host, "host must not be null");
        if (host.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535");
        }
    }
}
