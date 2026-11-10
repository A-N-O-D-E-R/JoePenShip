package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.support.FakeImageProvider;
import io.virtualization.sdk.cli.support.FakeVirtualizationProvider;
import io.virtualization.sdk.core.ProviderRegistry;
import io.virtualization.sdk.core.VirtualizationClient;
import io.virtualization.sdk.core.image.ImageProviderRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Cross-invocation proof, exact {@code ProjectCommandsTest} shape — {@code JsonFileNetworkStore} bridges separate CLI runs. */
class SubnetCommandsTest {

    private String configFlag;
    private StringWriter outSink;
    private StringWriter errSink;
    private CommandLine commandLine;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        VirtualizationClient client = new VirtualizationClient(
                new ProviderRegistry(Map.of("test", new FakeVirtualizationProvider())),
                new ImageProviderRegistry(Map.of("test", new FakeImageProvider())));
        configFlag = tempDir.resolve("config.yaml").toString();
        outSink = new StringWriter();
        errSink = new StringWriter();
        commandLine = new CommandLine(new VirtualizationCli(path -> client))
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

    private String createNetwork() {
        execute("network", "create", "--name", "public", "--config", configFlag, "--output", "json");
        return NetworkCommandsTest.extractId(outSink.toString());
    }

    @Test
    void createSucceedsAndPrintsASubnetId() {
        String networkId = createNetwork();

        int code = execute("subnet", "create", "--network", networkId, "--cidr", "10.0.0.0/24", "--gateway", "10.0.0.1", "--config", configFlag, "--output", "json");

        assertThat(code).isZero();
        assertThat(outSink.toString()).contains("10.0.0.0/24").contains("10.0.0.1");
    }

    @Test
    void createRejectsAnOverlappingCidrInTheSameNetwork() {
        String networkId = createNetwork();
        execute("subnet", "create", "--network", networkId, "--cidr", "10.0.0.0/24", "--config", configFlag);

        int code = execute("subnet", "create", "--network", networkId, "--cidr", "10.0.0.128/25", "--config", configFlag);

        assertThat(code).isEqualTo(ExitCodes.GENERAL_ERROR);
    }

    @Test
    void listAndGetSeeASubnetCreatedInAnEarlierInvocation() {
        String networkId = createNetwork();
        execute("subnet", "create", "--network", networkId, "--cidr", "10.0.0.0/24", "--config", configFlag, "--output", "json");
        String id = NetworkCommandsTest.extractId(outSink.toString());

        int listCode = execute("subnet", "list", "--network", networkId, "--config", configFlag, "--output", "json");
        assertThat(listCode).isZero();
        assertThat(outSink.toString()).contains("10.0.0.0/24").contains(id);

        int getCode = execute("subnet", "get", id, "--config", configFlag, "--output", "json");
        assertThat(getCode).isZero();
        assertThat(outSink.toString()).contains("10.0.0.0/24");
    }

    @Test
    void getUnknownSubnetReturnsResourceNotFoundExitCode() {
        int code = execute("subnet", "get", "subnet-does-not-exist", "--config", configFlag);

        assertThat(code).isEqualTo(ExitCodes.RESOURCE_NOT_FOUND);
    }

    @Test
    void deleteRemovesTheSubnetFromSubsequentListings() {
        String networkId = createNetwork();
        execute("subnet", "create", "--network", networkId, "--cidr", "10.0.0.0/24", "--config", configFlag, "--output", "json");
        String id = NetworkCommandsTest.extractId(outSink.toString());

        int deleteCode = execute("subnet", "delete", id, "--config", configFlag);
        assertThat(deleteCode).isZero();

        int listCode = execute("subnet", "list", "--network", networkId, "--config", configFlag, "--output", "json");
        assertThat(listCode).isZero();
        assertThat(outSink.toString()).doesNotContain(id);
    }
}
