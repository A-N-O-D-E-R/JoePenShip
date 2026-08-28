package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.loadbalancer.LoadBalancerProvider;
import io.virtualization.sdk.loadbalancer.LoadBalancerProviderRegistry;
import io.virtualization.sdk.loadbalancer.Route;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/** {@code /api/v1/load-balancers/{name}/routes}, over {@link LoadBalancerProviderRegistry}. */
@RestController
@RequestMapping("/api/v1/load-balancers/{name}/routes")
public class RouteController {

    private final LoadBalancerProviderRegistry registry;

    public RouteController(LoadBalancerProviderRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    public List<Route> list(@PathVariable String name, @RequestParam String provider) {
        return provider(provider).listRoutes(name);
    }

    @PostMapping
    public Route create(@PathVariable String name, @RequestParam String provider, @RequestBody CreateRouteRequestBody request) {
        Objects.requireNonNull(request.host(), "'host' is required");
        Objects.requireNonNull(request.backends(), "'backends' is required");
        return provider(provider).createRoute(name, request.host(), request.backends(), request.healthCheck());
    }

    @GetMapping("/{host}")
    public Route get(@PathVariable String name, @PathVariable String host, @RequestParam String provider) {
        return provider(provider).getRoute(name, host);
    }

    @PutMapping("/{host}")
    public Route update(
            @PathVariable String name, @PathVariable String host, @RequestParam String provider,
            @RequestBody CreateRouteRequestBody request) {
        Objects.requireNonNull(request.backends(), "'backends' is required");
        return provider(provider).updateRoute(name, host, request.backends(), request.healthCheck());
    }

    @DeleteMapping("/{host}")
    public void delete(@PathVariable String name, @PathVariable String host, @RequestParam String provider) {
        provider(provider).deleteRoute(name, host);
    }

    private LoadBalancerProvider provider(String name) {
        return registry.get(name);
    }
}
