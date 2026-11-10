package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.loadbalancer.LoadBalancer;
import io.virtualization.sdk.loadbalancer.LoadBalancerProviderRegistry;
import io.virtualization.sdk.spring.web.support.FakeLoadBalancerProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoadBalancerControllerTest {

    private LoadBalancerController controller;

    @BeforeEach
    void setUp() {
        LoadBalancerProviderRegistry registry = new LoadBalancerProviderRegistry(Map.of("caddy", new FakeLoadBalancerProvider()));
        controller = new LoadBalancerController(registry);
    }

    @Test
    void createReturnsTheCreatedLoadBalancer() {
        LoadBalancer created = controller.create("caddy", new CreateLoadBalancerRequestBody("web", List.of(":443")));

        assertThat(created.name()).isEqualTo("web");
        assertThat(created.listenAddresses()).containsExactly(":443");
    }

    @Test
    void getReturnsTheCreatedLoadBalancer() {
        controller.create("caddy", new CreateLoadBalancerRequestBody("web", List.of(":443")));

        assertThat(controller.get("web", "caddy").name()).isEqualTo("web");
    }

    @Test
    void getUnknownLoadBalancerThrowsResourceNotFound() {
        assertThatThrownBy(() -> controller.get("missing", "caddy")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listReturnsEveryCreatedLoadBalancer() {
        controller.create("caddy", new CreateLoadBalancerRequestBody("web", List.of(":443")));

        assertThat(controller.list("caddy")).extracting(LoadBalancer::name).containsExactly("web");
    }

    @Test
    void deleteRemovesTheLoadBalancer() {
        controller.create("caddy", new CreateLoadBalancerRequestBody("web", List.of(":443")));

        controller.delete("web", "caddy");

        assertThat(controller.list("caddy")).isEmpty();
    }
}
