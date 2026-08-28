package io.virtualization.sdk.loadbalancer;

import io.virtualization.sdk.core.exception.ResourceNotFoundException;

import java.util.List;

/**
 * A load-balancer backend (Caddy, ...). Implementations live in their own provider modules — this
 * module never depends on one.
 */
public interface LoadBalancerProvider {

    List<LoadBalancer> listLoadBalancers();

    /** @throws ResourceNotFoundException if {@code name} is unknown */
    LoadBalancer getLoadBalancer(String name);

    /** @throws IllegalArgumentException if {@code name} is already taken */
    LoadBalancer createLoadBalancer(String name, List<String> listenAddresses);

    /** @throws ResourceNotFoundException if {@code name} is unknown */
    void deleteLoadBalancer(String name);

    /** @throws ResourceNotFoundException if {@code loadBalancerName} is unknown */
    List<Route> listRoutes(String loadBalancerName);

    /** @throws ResourceNotFoundException if {@code loadBalancerName} or {@code host} is unknown */
    Route getRoute(String loadBalancerName, String host);

    /**
     * @throws ResourceNotFoundException if {@code loadBalancerName} is unknown
     * @throws IllegalArgumentException if {@code host} already has a route on this load balancer
     */
    Route createRoute(String loadBalancerName, String host, List<Backend> backends, HealthCheck healthCheck);

    /** Replaces the route's backends/health check wholesale. @throws ResourceNotFoundException if {@code loadBalancerName} or {@code host} is unknown */
    Route updateRoute(String loadBalancerName, String host, List<Backend> backends, HealthCheck healthCheck);

    /** @throws ResourceNotFoundException if {@code loadBalancerName} or {@code host} is unknown */
    void deleteRoute(String loadBalancerName, String host);
}
