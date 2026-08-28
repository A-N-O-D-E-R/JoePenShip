package io.virtualization.sdk.cli.support;

import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.loadbalancer.Backend;
import io.virtualization.sdk.loadbalancer.HealthCheck;
import io.virtualization.sdk.loadbalancer.LoadBalancer;
import io.virtualization.sdk.loadbalancer.LoadBalancerProvider;
import io.virtualization.sdk.loadbalancer.Route;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Hand-written in-memory test double. */
public final class FakeLoadBalancerProvider implements LoadBalancerProvider {

    private final Map<String, LoadBalancer> loadBalancers = new ConcurrentHashMap<>();
    private final Map<String, Route> routes = new ConcurrentHashMap<>();

    private static String routeKey(String loadBalancerName, String host) {
        return loadBalancerName + "/" + host;
    }

    @Override
    public List<LoadBalancer> listLoadBalancers() {
        return List.copyOf(loadBalancers.values());
    }

    @Override
    public LoadBalancer getLoadBalancer(String name) {
        LoadBalancer loadBalancer = loadBalancers.get(name);
        if (loadBalancer == null) {
            throw new ResourceNotFoundException("No load balancer named '" + name + "'");
        }
        return loadBalancer;
    }

    @Override
    public LoadBalancer createLoadBalancer(String name, List<String> listenAddresses) {
        if (loadBalancers.containsKey(name)) {
            throw new IllegalArgumentException("load balancer name '" + name + "' is already taken");
        }
        LoadBalancer loadBalancer = new LoadBalancer(name, listenAddresses);
        loadBalancers.put(name, loadBalancer);
        return loadBalancer;
    }

    @Override
    public void deleteLoadBalancer(String name) {
        getLoadBalancer(name);
        loadBalancers.remove(name);
        routes.keySet().removeIf(key -> key.startsWith(name + "/"));
    }

    @Override
    public List<Route> listRoutes(String loadBalancerName) {
        getLoadBalancer(loadBalancerName);
        return routes.values().stream().filter(r -> r.loadBalancerName().equals(loadBalancerName)).toList();
    }

    @Override
    public Route getRoute(String loadBalancerName, String host) {
        Route route = routes.get(routeKey(loadBalancerName, host));
        if (route == null) {
            throw new ResourceNotFoundException("No route for host '" + host + "' on load balancer '" + loadBalancerName + "'");
        }
        return route;
    }

    @Override
    public Route createRoute(String loadBalancerName, String host, List<Backend> backends, HealthCheck healthCheck) {
        getLoadBalancer(loadBalancerName);
        String key = routeKey(loadBalancerName, host);
        if (routes.containsKey(key)) {
            throw new IllegalArgumentException("host '" + host + "' already has a route on load balancer '" + loadBalancerName + "'");
        }
        Route route = new Route(loadBalancerName, host, backends, healthCheck);
        routes.put(key, route);
        return route;
    }

    @Override
    public Route updateRoute(String loadBalancerName, String host, List<Backend> backends, HealthCheck healthCheck) {
        getRoute(loadBalancerName, host);
        Route route = new Route(loadBalancerName, host, backends, healthCheck);
        routes.put(routeKey(loadBalancerName, host), route);
        return route;
    }

    @Override
    public void deleteRoute(String loadBalancerName, String host) {
        getRoute(loadBalancerName, host);
        routes.remove(routeKey(loadBalancerName, host));
    }
}
