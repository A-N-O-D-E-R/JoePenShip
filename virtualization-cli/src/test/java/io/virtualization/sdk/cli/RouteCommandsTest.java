package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.support.FakeImageProvider;
import io.virtualization.sdk.cli.support.FakeLoadBalancerProvider;
import io.virtualization.sdk.cli.support.FakeVirtualizationProvider;
import io.virtualization.sdk.core.ProviderRegistry;
import io.virtualization.sdk.core.VirtualizationClient;
import io.virtualization.sdk.core.image.ImageProviderRegistry;
import io.virtualization.sdk.loadbalancer.LoadBalancerProviderRegistry;
import io.virtualization.sdk.storage.StorageProviderRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RouteCommandsTest {

    private StringWriter outSink;
    private StringWriter errSink;
    private CommandLine commandLine;

    @BeforeEach
    void setUp() {
        VirtualizationClient client = new VirtualizationClient(
                new ProviderRegistry(Map.of("test", new FakeVirtualizationProvider())),
                new ImageProviderRegistry(Map.of("test", new FakeImageProvider())));
        StorageProviderRegistry storageProviderRegistry = new StorageProviderRegistry(Map.of());
        LoadBalancerProviderRegistry loadBalancerProviderRegistry =
                new LoadBalancerProviderRegistry(Map.of("caddy", new FakeLoadBalancerProvider()));
        outSink = new StringWriter();
        errSink = new StringWriter();
        commandLine = new CommandLine(new VirtualizationCli(path -> client, path -> storageProviderRegistry, path -> loadBalancerProviderRegistry))
                .setExecutionExceptionHandler(new CliExceptionHandler())
                .setCaseInsensitiveEnumValuesAllowed(true)
                .setOut(new PrintWriter(outSink))
                .setErr(new PrintWriter(errSink));
        execute("load-balancer", "create", "web", "--listen", ":443", "--provider", "caddy");
    }

    private int execute(String... args) {
        outSink.getBuffer().setLength(0);
        errSink.getBuffer().setLength(0);
        int code = commandLine.execute(args);
        commandLine.getOut().flush();
        commandLine.getErr().flush();
        return code;
    }

    @Test
    void createSucceedsAndPrintsTheRoute() {
        int code = execute(
                "route", "create", "app.example.com", "--load-balancer", "web", "--backend", "10.0.0.1:8080",
                "--backend", "10.0.0.2:8080", "--provider", "caddy", "--output", "json");

        assertThat(code).isZero();
        assertThat(outSink.toString()).contains("app.example.com").contains("10.0.0.1");
    }

    @Test
    void createWithHealthCheck() {
        int code = execute(
                "route", "create", "app.example.com", "--load-balancer", "web", "--backend", "10.0.0.1:8080",
                "--health-check-path", "/healthz", "--health-check-interval", "10", "--health-check-timeout", "5",
                "--provider", "caddy", "--output", "json");

        assertThat(code).isZero();
        assertThat(outSink.toString()).contains("/healthz");
    }

    @Test
    void listAndGetSeeARouteCreatedInAnEarlierInvocation() {
        execute("route", "create", "app.example.com", "--load-balancer", "web", "--backend", "10.0.0.1:8080", "--provider", "caddy");

        int listCode = execute("route", "list", "--load-balancer", "web", "--provider", "caddy", "--output", "json");
        assertThat(listCode).isZero();
        assertThat(outSink.toString()).contains("app.example.com");

        int getCode = execute("route", "get", "app.example.com", "--load-balancer", "web", "--provider", "caddy", "--output", "json");
        assertThat(getCode).isZero();
        assertThat(outSink.toString()).contains("app.example.com");
    }

    @Test
    void getUnknownRouteReturnsResourceNotFoundExitCode() {
        int code = execute("route", "get", "missing.example.com", "--load-balancer", "web", "--provider", "caddy");

        assertThat(code).isEqualTo(ExitCodes.RESOURCE_NOT_FOUND);
    }

    @Test
    void updateReplacesTheBackendList() {
        execute("route", "create", "app.example.com", "--load-balancer", "web", "--backend", "10.0.0.1:8080", "--provider", "caddy");

        int code = execute(
                "route", "update", "app.example.com", "--load-balancer", "web", "--backend", "10.0.0.3:9090", "--provider", "caddy",
                "--output", "json");

        assertThat(code).isZero();
        assertThat(outSink.toString()).contains("10.0.0.3").doesNotContain("10.0.0.1");
    }

    @Test
    void deleteRemovesTheRouteFromSubsequentListings() {
        execute("route", "create", "app.example.com", "--load-balancer", "web", "--backend", "10.0.0.1:8080", "--provider", "caddy");

        int deleteCode = execute("route", "delete", "app.example.com", "--load-balancer", "web", "--provider", "caddy");
        assertThat(deleteCode).isZero();

        int listCode = execute("route", "list", "--load-balancer", "web", "--provider", "caddy", "--output", "json");
        assertThat(listCode).isZero();
        assertThat(outSink.toString()).doesNotContain("app.example.com");
    }
}
