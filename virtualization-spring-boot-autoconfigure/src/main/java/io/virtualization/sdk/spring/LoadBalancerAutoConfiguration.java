package io.virtualization.sdk.spring;

import io.virtualization.sdk.caddy.CaddyClientConfig;
import io.virtualization.sdk.caddy.CaddyLoadBalancerProvider;
import io.virtualization.sdk.core.exception.ConfigurationException;
import io.virtualization.sdk.loadbalancer.LoadBalancerProvider;
import io.virtualization.sdk.loadbalancer.LoadBalancerProviderRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Wires a {@link LoadBalancerProviderRegistry} from {@code virtualization.loadbalancers.providers.*}
 * — always created (possibly empty), same "always on" stance {@link NetworkAutoConfiguration}
 * takes: a load balancer needs no compute-provider configuration to make sense. Only {@code type:
 * caddy} exists — see {@link LoadBalancerProperties}.
 */
@AutoConfiguration
@EnableConfigurationProperties(LoadBalancerProperties.class)
public class LoadBalancerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public LoadBalancerProviderRegistry loadBalancerProviderRegistry(LoadBalancerProperties properties) {
        Map<String, LoadBalancerProvider> providers = new LinkedHashMap<>();
        properties.providers().forEach((name, entry) -> providers.put(name, createLoadBalancerProvider(name, entry)));
        return new LoadBalancerProviderRegistry(providers);
    }

    private static LoadBalancerProvider createLoadBalancerProvider(String name, LoadBalancerProperties.ProviderEntry entry) {
        if (entry.type() == null || entry.type().isBlank()) {
            throw new ConfigurationException("Load balancer '" + name + "' is missing required field 'type'.");
        }
        if (!"caddy".equals(entry.type())) {
            throw new ConfigurationException("Load balancer '" + name + "' has unknown type '" + entry.type() + "'");
        }
        URI endpoint = entry.endpoint() != null ? URI.create(entry.endpoint()) : URI.create("http://localhost:2019");
        return new CaddyLoadBalancerProvider(new CaddyClientConfig(endpoint, Duration.ofSeconds(5), Duration.ofSeconds(10)));
    }
}
