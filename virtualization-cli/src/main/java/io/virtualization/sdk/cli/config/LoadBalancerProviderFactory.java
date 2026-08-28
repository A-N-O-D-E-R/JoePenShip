package io.virtualization.sdk.cli.config;

import io.virtualization.sdk.caddy.CaddyClientConfig;
import io.virtualization.sdk.caddy.CaddyLoadBalancerProvider;
import io.virtualization.sdk.core.exception.ConfigurationException;
import io.virtualization.sdk.loadbalancer.LoadBalancerProvider;

import java.net.URI;
import java.time.Duration;

/** Builds a {@link LoadBalancerProvider} from one {@link LoadBalancerProviderEntry}. */
final class LoadBalancerProviderFactory {

    private LoadBalancerProviderFactory() {}

    static LoadBalancerProvider create(String name, LoadBalancerProviderEntry entry) {
        if (entry.type() == null || entry.type().isBlank()) {
            throw new ConfigurationException("Load balancer '" + name + "' is missing required field 'type'.");
        }
        return switch (entry.type()) {
            case "caddy" -> createCaddy(entry);
            default -> throw new ConfigurationException("Load balancer '" + name + "' has unknown type '" + entry.type() + "'");
        };
    }

    private static LoadBalancerProvider createCaddy(LoadBalancerProviderEntry entry) {
        URI endpoint = entry.endpoint() != null ? URI.create(entry.endpoint()) : URI.create("http://localhost:2019");
        return new CaddyLoadBalancerProvider(new CaddyClientConfig(endpoint, Duration.ofSeconds(5), Duration.ofSeconds(10)));
    }
}
