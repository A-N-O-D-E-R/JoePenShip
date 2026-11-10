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

class LoadBalancerCommandsTest {

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
    void createSucceedsAndPrintsTheLoadBalancer() {
        int code = execute("load-balancer", "create", "web", "--listen", ":443", "--provider", "caddy", "--output", "json");

        assertThat(code).isZero();
        assertThat(outSink.toString()).contains("web").contains(":443");
    }

    @Test
    void createRejectsADuplicateName() {
        execute("load-balancer", "create", "web", "--listen", ":443", "--provider", "caddy");

        int code = execute("load-balancer", "create", "web", "--listen", ":80", "--provider", "caddy");

        assertThat(code).isEqualTo(ExitCodes.GENERAL_ERROR);
    }

    @Test
    void listAndGetSeeALoadBalancerCreatedInAnEarlierInvocation() {
        execute("load-balancer", "create", "web", "--listen", ":443", "--provider", "caddy");

        int listCode = execute("load-balancer", "list", "--provider", "caddy", "--output", "json");
        assertThat(listCode).isZero();
        assertThat(outSink.toString()).contains("web");

        int getCode = execute("load-balancer", "get", "web", "--provider", "caddy", "--output", "json");
        assertThat(getCode).isZero();
        assertThat(outSink.toString()).contains("web");
    }

    @Test
    void getUnknownLoadBalancerReturnsResourceNotFoundExitCode() {
        int code = execute("load-balancer", "get", "missing", "--provider", "caddy");

        assertThat(code).isEqualTo(ExitCodes.RESOURCE_NOT_FOUND);
    }

    @Test
    void deleteRemovesTheLoadBalancerFromSubsequentListings() {
        execute("load-balancer", "create", "web", "--listen", ":443", "--provider", "caddy");

        int deleteCode = execute("load-balancer", "delete", "web", "--provider", "caddy");
        assertThat(deleteCode).isZero();

        int listCode = execute("load-balancer", "list", "--provider", "caddy", "--output", "json");
        assertThat(listCode).isZero();
        assertThat(outSink.toString()).doesNotContain("\"name\" : \"web\"");
    }
}
