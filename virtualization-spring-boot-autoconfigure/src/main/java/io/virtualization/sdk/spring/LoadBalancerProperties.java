package io.virtualization.sdk.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Binds:
 * <pre>{@code
 * virtualization:
 *   loadbalancers:
 *     providers:
 *       local:
 *         type: caddy
 *         endpoint: http://localhost:2019   # optional, default http://localhost:2019
 * }</pre>
 * Its own config surface, not part of {@code virtualization.providers.*} — Caddy isn't a
 * compute/image/storage backend. Only {@code type: caddy} exists today; a real provider type is a
 * later addition to {@link LoadBalancerAutoConfiguration}, not a rewrite of this shape.
 */
@ConfigurationProperties(prefix = "virtualization.loadbalancers")
public record LoadBalancerProperties(Map<String, ProviderEntry> providers) {

    public LoadBalancerProperties {
        providers = providers != null ? Map.copyOf(providers) : Map.of();
    }

    public record ProviderEntry(String type, String endpoint) {}
}
