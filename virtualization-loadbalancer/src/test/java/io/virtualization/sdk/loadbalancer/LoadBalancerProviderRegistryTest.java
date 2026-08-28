package io.virtualization.sdk.loadbalancer;

import io.virtualization.sdk.core.exception.ConfigurationException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoadBalancerProviderRegistryTest {

    private final LoadBalancerProvider provider = new NoOpLoadBalancerProvider();

    @Test
    void getReturnsRegisteredProvider() {
        LoadBalancerProviderRegistry registry = new LoadBalancerProviderRegistry(Map.of("caddy", provider));

        assertThat(registry.get("caddy")).isSameAs(provider);
    }

    @Test
    void getThrowsConfigurationExceptionForUnknownName() {
        LoadBalancerProviderRegistry registry = new LoadBalancerProviderRegistry(Map.of("caddy", provider));

        assertThatThrownBy(() -> registry.get("traefik")).isInstanceOf(ConfigurationException.class);
    }

    @Test
    void allIsUnmodifiableAndComplete() {
        LoadBalancerProviderRegistry registry = new LoadBalancerProviderRegistry(Map.of("caddy", provider));

        assertThat(registry.all()).containsExactly(Map.entry("caddy", provider));
        assertThat(registry.all()).isUnmodifiable();
    }

    @Test
    void rejectsBlankProviderName() {
        Map<String, LoadBalancerProvider> providers = new HashMap<>();
        providers.put(" ", provider);

        assertThatIllegalArgumentException().isThrownBy(() -> new LoadBalancerProviderRegistry(providers));
    }

    @Test
    void rejectsNullProvider() {
        Map<String, LoadBalancerProvider> providers = new HashMap<>();
        providers.put("caddy", null);

        assertThatNullPointerException().isThrownBy(() -> new LoadBalancerProviderRegistry(providers));
    }

    @Test
    void rejectsNullMap() {
        assertThatNullPointerException().isThrownBy(() -> new LoadBalancerProviderRegistry(null));
    }

    private static final class NoOpLoadBalancerProvider implements LoadBalancerProvider {
        @Override
        public List<LoadBalancer> listLoadBalancers() {
            return List.of();
        }

        @Override
        public LoadBalancer getLoadBalancer(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public LoadBalancer createLoadBalancer(String name, List<String> listenAddresses) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteLoadBalancer(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Route> listRoutes(String loadBalancerName) {
            return List.of();
        }

        @Override
        public Route getRoute(String loadBalancerName, String host) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Route createRoute(String loadBalancerName, String host, List<Backend> backends, HealthCheck healthCheck) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Route updateRoute(String loadBalancerName, String host, List<Backend> backends, HealthCheck healthCheck) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteRoute(String loadBalancerName, String host) {
            throw new UnsupportedOperationException();
        }
    }
}
