package io.virtualization.sdk.loadbalancer;

import java.util.List;
import java.util.Objects;

/**
 * A named load balancer — a set of listen addresses fronting host-matched {@link Route}s.
 * {@code name} is the identifier; unique (enforced by {@link LoadBalancerProvider#createLoadBalancer}).
 */
public record LoadBalancer(String name, List<String> listenAddresses) {

    public LoadBalancer {
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        Objects.requireNonNull(listenAddresses, "listenAddresses must not be null");
        if (listenAddresses.isEmpty()) {
            throw new IllegalArgumentException("listenAddresses must not be empty");
        }
        listenAddresses = List.copyOf(listenAddresses);
    }
}
