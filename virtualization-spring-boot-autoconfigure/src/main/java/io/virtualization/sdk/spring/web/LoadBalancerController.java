package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.loadbalancer.LoadBalancer;
import io.virtualization.sdk.loadbalancer.LoadBalancerProviderRegistry;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/**
 * {@code /api/v1/load-balancers}, over {@link LoadBalancerProviderRegistry}. {@code provider}
 * selects which configured entry to use, same role as {@link StoragePoolController}/{@link
 * ImageController} — but resolved against a separate namespace ({@code
 * virtualization.loadbalancers.providers.*}, not {@code virtualization.providers.*}).
 */
@RestController
@RequestMapping("/api/v1/load-balancers")
public class LoadBalancerController {

    private final LoadBalancerProviderRegistry registry;

    public LoadBalancerController(LoadBalancerProviderRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    public List<LoadBalancer> list(@RequestParam String provider) {
        return registry.get(provider).listLoadBalancers();
    }

    @PostMapping
    public LoadBalancer create(@RequestParam String provider, @RequestBody CreateLoadBalancerRequestBody request) {
        Objects.requireNonNull(request.name(), "'name' is required");
        Objects.requireNonNull(request.listenAddresses(), "'listenAddresses' is required");
        return registry.get(provider).createLoadBalancer(request.name(), request.listenAddresses());
    }

    @GetMapping("/{name}")
    public LoadBalancer get(@PathVariable String name, @RequestParam String provider) {
        return registry.get(provider).getLoadBalancer(name);
    }

    @DeleteMapping("/{name}")
    public void delete(@PathVariable String name, @RequestParam String provider) {
        registry.get(provider).deleteLoadBalancer(name);
    }
}
