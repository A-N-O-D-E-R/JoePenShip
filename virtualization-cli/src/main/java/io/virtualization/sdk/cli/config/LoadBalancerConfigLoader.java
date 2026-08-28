package io.virtualization.sdk.cli.config;

import com.fasterxml.jackson.databind.JsonNode;
import io.virtualization.sdk.loadbalancer.LoadBalancerProvider;
import io.virtualization.sdk.loadbalancer.LoadBalancerProviderRegistry;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads {@code virtualization.loadbalancers.providers.<name>} — its own config surface, not
 * reused from {@code virtualization.providers.*} (Caddy isn't a compute/image/storage backend).
 * Mirrors {@link DnsConfigLoader}'s {@code virtualization.dns.providers.<name>} shape exactly.
 */
public final class LoadBalancerConfigLoader {

    private LoadBalancerConfigLoader() {}

    public static LoadBalancerProviderRegistry loadRegistry(Path configPath) {
        Map<String, LoadBalancerProvider> providers = new LinkedHashMap<>();
        JsonNode section = ConfigYaml.section(configPath, "virtualization", "loadbalancers", "providers");
        section.fields().forEachRemaining(field -> {
            String name = field.getKey();
            LoadBalancerProviderEntry entry = ConfigYaml.MAPPER.convertValue(field.getValue(), LoadBalancerProviderEntry.class);
            providers.put(name, LoadBalancerProviderFactory.create(name, entry));
        });
        return new LoadBalancerProviderRegistry(providers);
    }
}
