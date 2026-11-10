package io.virtualization.sdk.loadbalancer;

import java.util.List;
import java.util.Objects;

/**
 * One virtual-host route within a {@link LoadBalancer}: requests matching {@code Host: <host>}
 * are reverse-proxied across {@code backends}. {@code healthCheck} is nullable — no active
 * health checking.
 */
public record Route(String loadBalancerName, String host, List<Backend> backends, HealthCheck healthCheck) {

    public Route {
        Objects.requireNonNull(loadBalancerName, "loadBalancerName must not be null");
        Objects.requireNonNull(host, "host must not be null");
        if (host.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }
        Objects.requireNonNull(backends, "backends must not be null");
        if (backends.isEmpty()) {
            throw new IllegalArgumentException("backends must not be empty");
        }
        backends = List.copyOf(backends);
    }
}
