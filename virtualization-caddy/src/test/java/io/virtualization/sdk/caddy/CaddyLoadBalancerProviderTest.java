package io.virtualization.sdk.caddy;

import io.virtualization.sdk.caddy.client.CaddyApiClient;
import io.virtualization.sdk.caddy.support.FakeCaddyServer;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.loadbalancer.Backend;
import io.virtualization.sdk.loadbalancer.HealthCheck;
import io.virtualization.sdk.loadbalancer.LoadBalancer;
import io.virtualization.sdk.loadbalancer.Route;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CaddyLoadBalancerProviderTest {

    private FakeCaddyServer server;
    private CaddyLoadBalancerProvider provider;

    @BeforeEach
    void startServer() {
        server = new FakeCaddyServer().start();
        CaddyApiClient client = new CaddyApiClient(HttpClient.newHttpClient(), server.uri(), Duration.ofSeconds(5));
        provider = new CaddyLoadBalancerProvider(client);
    }

    @AfterEach
    void stopServer() {
        server.close();
    }

    @Test
    void createLoadBalancerThenGetRoundTrips() {
        LoadBalancer created = provider.createLoadBalancer("web", List.of(":443"));

        assertThat(created.name()).isEqualTo("web");
        assertThat(created.listenAddresses()).containsExactly(":443");
        assertThat(provider.getLoadBalancer("web")).isEqualTo(created);
    }

    @Test
    void createLoadBalancerRejectsADuplicateName() {
        provider.createLoadBalancer("web", List.of(":443"));

        assertThatThrownBy(() -> provider.createLoadBalancer("web", List.of(":80"))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getUnknownLoadBalancerThrowsResourceNotFound() {
        assertThatThrownBy(() -> provider.getLoadBalancer("missing")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listLoadBalancersReturnsEveryCreatedOne() {
        provider.createLoadBalancer("web", List.of(":443"));
        provider.createLoadBalancer("api", List.of(":8443"));

        assertThat(provider.listLoadBalancers()).extracting(LoadBalancer::name).containsExactlyInAnyOrder("web", "api");
    }

    @Test
    void deleteLoadBalancerRemovesIt() {
        provider.createLoadBalancer("web", List.of(":443"));

        provider.deleteLoadBalancer("web");

        assertThatThrownBy(() -> provider.getLoadBalancer("web")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteUnknownLoadBalancerThrowsResourceNotFound() {
        assertThatThrownBy(() -> provider.deleteLoadBalancer("missing")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createRouteThenGetRoundTrips() {
        provider.createLoadBalancer("web", List.of(":443"));
        List<Backend> backends = List.of(new Backend("10.0.0.1", 8080), new Backend("10.0.0.2", 8080));
        HealthCheck healthCheck = new HealthCheck("/healthz", Duration.ofSeconds(10), Duration.ofSeconds(5));

        Route created = provider.createRoute("web", "app.example.com", backends, healthCheck);

        assertThat(created.backends()).containsExactlyElementsOf(backends);
        assertThat(created.healthCheck()).isEqualTo(healthCheck);
        assertThat(provider.getRoute("web", "app.example.com")).isEqualTo(created);
    }

    @Test
    void createRouteWithoutHealthCheck() {
        provider.createLoadBalancer("web", List.of(":443"));

        Route created = provider.createRoute("web", "app.example.com", List.of(new Backend("10.0.0.1", 8080)), null);

        assertThat(created.healthCheck()).isNull();
        assertThat(provider.getRoute("web", "app.example.com").healthCheck()).isNull();
    }

    @Test
    void createRouteRejectsADuplicateHost() {
        provider.createLoadBalancer("web", List.of(":443"));
        provider.createRoute("web", "app.example.com", List.of(new Backend("10.0.0.1", 8080)), null);

        assertThatThrownBy(() -> provider.createRoute("web", "app.example.com", List.of(new Backend("10.0.0.2", 8080)), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createRouteThrowsResourceNotFoundForUnknownLoadBalancer() {
        assertThatThrownBy(() -> provider.createRoute("missing", "app.example.com", List.of(new Backend("10.0.0.1", 8080)), null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listRoutesReturnsEveryCreatedRoute() {
        provider.createLoadBalancer("web", List.of(":443"));
        provider.createRoute("web", "app.example.com", List.of(new Backend("10.0.0.1", 8080)), null);
        provider.createRoute("web", "api.example.com", List.of(new Backend("10.0.0.2", 8080)), null);

        assertThat(provider.listRoutes("web")).extracting(Route::host).containsExactlyInAnyOrder("app.example.com", "api.example.com");
    }

    @Test
    void updateRouteReplacesTheBackendListWholesale() {
        provider.createLoadBalancer("web", List.of(":443"));
        provider.createRoute("web", "app.example.com", List.of(new Backend("10.0.0.1", 8080)), null);

        Route updated = provider.updateRoute("web", "app.example.com", List.of(new Backend("10.0.0.3", 9090)), null);

        assertThat(updated.backends()).containsExactly(new Backend("10.0.0.3", 9090));
        Route fetched = provider.getRoute("web", "app.example.com");
        assertThat(fetched.backends()).containsExactly(new Backend("10.0.0.3", 9090));
    }

    @Test
    void updateRouteThrowsResourceNotFoundForUnknownRoute() {
        provider.createLoadBalancer("web", List.of(":443"));

        assertThatThrownBy(() -> provider.updateRoute("web", "missing.example.com", List.of(new Backend("10.0.0.1", 8080)), null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteRouteRemovesItFromSubsequentListings() {
        provider.createLoadBalancer("web", List.of(":443"));
        provider.createRoute("web", "app.example.com", List.of(new Backend("10.0.0.1", 8080)), null);

        provider.deleteRoute("web", "app.example.com");

        assertThat(provider.listRoutes("web")).isEmpty();
    }

    @Test
    void deleteRouteThrowsResourceNotFoundForUnknownRoute() {
        provider.createLoadBalancer("web", List.of(":443"));

        assertThatThrownBy(() -> provider.deleteRoute("web", "missing.example.com")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void multipleLoadBalancersDoNotInterfereWithEachOthersRoutes() {
        provider.createLoadBalancer("web", List.of(":443"));
        provider.createLoadBalancer("api", List.of(":8443"));
        provider.createRoute("web", "app.example.com", List.of(new Backend("10.0.0.1", 8080)), null);

        assertThat(provider.listRoutes("api")).isEmpty();
        assertThat(provider.listRoutes("web")).hasSize(1);
    }
}
