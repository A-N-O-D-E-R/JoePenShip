package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.loadbalancer.Backend;
import io.virtualization.sdk.loadbalancer.LoadBalancerProviderRegistry;
import io.virtualization.sdk.loadbalancer.Route;
import io.virtualization.sdk.spring.web.support.FakeLoadBalancerProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RouteControllerTest {

    private RouteController controller;

    @BeforeEach
    void setUp() {
        LoadBalancerProviderRegistry registry = new LoadBalancerProviderRegistry(Map.of("caddy", new FakeLoadBalancerProvider()));
        registry.get("caddy").createLoadBalancer("web", List.of(":443"));
        controller = new RouteController(registry);
    }

    @Test
    void createReturnsTheCreatedRoute() {
        Route created = controller.create(
                "web", "caddy", new CreateRouteRequestBody("app.example.com", List.of(new Backend("10.0.0.1", 8080)), null));

        assertThat(created.host()).isEqualTo("app.example.com");
        assertThat(created.backends()).containsExactly(new Backend("10.0.0.1", 8080));
    }

    @Test
    void getReturnsTheCreatedRoute() {
        controller.create("web", "caddy", new CreateRouteRequestBody("app.example.com", List.of(new Backend("10.0.0.1", 8080)), null));

        assertThat(controller.get("web", "app.example.com", "caddy").host()).isEqualTo("app.example.com");
    }

    @Test
    void getUnknownRouteThrowsResourceNotFound() {
        assertThatThrownBy(() -> controller.get("web", "missing.example.com", "caddy")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listReturnsEveryCreatedRoute() {
        controller.create("web", "caddy", new CreateRouteRequestBody("app.example.com", List.of(new Backend("10.0.0.1", 8080)), null));

        assertThat(controller.list("web", "caddy")).extracting(Route::host).containsExactly("app.example.com");
    }

    @Test
    void updateReplacesTheBackendListWholesale() {
        controller.create("web", "caddy", new CreateRouteRequestBody("app.example.com", List.of(new Backend("10.0.0.1", 8080)), null));

        Route updated = controller.update(
                "web", "app.example.com", "caddy", new CreateRouteRequestBody("app.example.com", List.of(new Backend("10.0.0.3", 9090)), null));

        assertThat(updated.backends()).containsExactly(new Backend("10.0.0.3", 9090));
    }

    @Test
    void deleteRemovesTheRoute() {
        controller.create("web", "caddy", new CreateRouteRequestBody("app.example.com", List.of(new Backend("10.0.0.1", 8080)), null));

        controller.delete("web", "app.example.com", "caddy");

        assertThat(controller.list("web", "caddy")).isEmpty();
    }
}
