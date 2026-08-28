package io.virtualization.sdk.loadbalancer;

import io.virtualization.sdk.core.exception.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

/**
 * An immutable, instance-based registry of named {@link LoadBalancerProvider}s. Mirrors {@code
 * io.virtualization.sdk.dns.DnsProviderRegistry} — the same pattern, for the load-balancer side
 * of the SDK.
 */
public final class LoadBalancerProviderRegistry {

    private static final Logger log = LoggerFactory.getLogger(LoadBalancerProviderRegistry.class);

    private final Map<String, LoadBalancerProvider> providers;

    public LoadBalancerProviderRegistry(Map<String, LoadBalancerProvider> providers) {
        Objects.requireNonNull(providers, "providers must not be null");
        for (Map.Entry<String, LoadBalancerProvider> entry : providers.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank()) {
                throw new IllegalArgumentException("provider name must not be blank");
            }
            Objects.requireNonNull(entry.getValue(), "provider must not be null for name '" + entry.getKey() + "'");
        }
        this.providers = Map.copyOf(providers);
        log.debug("Initialized load balancer provider registry with {} provider(s): {}", this.providers.size(), this.providers.keySet());
    }

    /**
     * @throws ConfigurationException if no load balancer provider is registered under the given name
     */
    public LoadBalancerProvider get(String name) {
        LoadBalancerProvider provider = providers.get(name);
        if (provider == null) {
            throw new ConfigurationException("No load balancer provider registered under name '" + name + "'");
        }
        return provider;
    }

    public Map<String, LoadBalancerProvider> all() {
        return providers;
    }
}
