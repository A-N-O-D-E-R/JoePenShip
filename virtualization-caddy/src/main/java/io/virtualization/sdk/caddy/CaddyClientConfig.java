package io.virtualization.sdk.caddy;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

/**
 * Connection settings for a {@link CaddyLoadBalancerProvider}. No TLS credentials modeled —
 * Caddy's admin API defaults to plaintext loopback-only ({@code http://localhost:2019}); a caller
 * fronting a remote admin API with TLS/mTLS reaches it via {@code endpoint} itself
 * ({@code https://...}), since the underlying client is transport-agnostic.
 */
public record CaddyClientConfig(URI endpoint, Duration connectTimeout, Duration requestTimeout) {

    public CaddyClientConfig {
        Objects.requireNonNull(endpoint, "endpoint must not be null");
        Objects.requireNonNull(connectTimeout, "connectTimeout must not be null");
        Objects.requireNonNull(requestTimeout, "requestTimeout must not be null");
        if (connectTimeout.isNegative() || connectTimeout.isZero()) {
            throw new IllegalArgumentException("connectTimeout must be positive");
        }
        if (requestTimeout.isNegative() || requestTimeout.isZero()) {
            throw new IllegalArgumentException("requestTimeout must be positive");
        }
    }

    /** Config for the default local Caddy admin API, with sensible default timeouts. */
    public static CaddyClientConfig local() {
        return new CaddyClientConfig(URI.create("http://localhost:2019"), Duration.ofSeconds(5), Duration.ofSeconds(10));
    }
}
