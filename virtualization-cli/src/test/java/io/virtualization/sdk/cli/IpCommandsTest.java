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
class IpCommandsTest {

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

    private String createSubnet(String cidr) {
        execute("network", "create", "--name", "public", "--config", configFlag, "--output", "json");
        String networkId = NetworkCommandsTest.extractId(outSink.toString());
        execute("subnet", "create", "--network", networkId, "--cidr", cidr, "--config", configFlag, "--output", "json");
        return NetworkCommandsTest.extractId(outSink.toString());
    }

    @Test
    void allocateReturnsTheFirstUsableAddress() {
        String subnetId = createSubnet("10.0.0.0/29");

        int code = execute("ip", "allocate", "--subnet", subnetId, "--owner", "vps-1", "--config", configFlag, "--output", "json");

        assertThat(code).isZero();
        assertThat(outSink.toString()).contains("10.0.0.1").contains("vps-1");
    }

    @Test
    void allocateExhaustsTheSubnetEventually() {
        String subnetId = createSubnet("10.0.0.0/30");
        execute("ip", "allocate", "--subnet", subnetId, "--config", configFlag);
        execute("ip", "allocate", "--subnet", subnetId, "--config", configFlag);

        int code = execute("ip", "allocate", "--subnet", subnetId, "--config", configFlag);

        assertThat(code).isEqualTo(ExitCodes.GENERAL_ERROR);
    }

    @Test
    void listSeesAnAllocationCreatedInAnEarlierInvocation() {
        String subnetId = createSubnet("10.0.0.0/29");
        execute("ip", "allocate", "--subnet", subnetId, "--owner", "vps-1", "--config", configFlag, "--output", "json");
        String id = NetworkCommandsTest.extractId(outSink.toString());

        int listCode = execute("ip", "list", "--subnet", subnetId, "--config", configFlag, "--output", "json");

        assertThat(listCode).isZero();
        assertThat(outSink.toString()).contains(id).contains("vps-1");
    }

    @Test
    void releaseFreesTheAddressForReallocation() {
        String subnetId = createSubnet("10.0.0.0/30");
        execute("ip", "allocate", "--subnet", subnetId, "--config", configFlag, "--output", "json");
        String id = NetworkCommandsTest.extractId(outSink.toString());

        int releaseCode = execute("ip", "release", id, "--config", configFlag);
        assertThat(releaseCode).isZero();

        int listCode = execute("ip", "list", "--subnet", subnetId, "--config", configFlag, "--output", "json");
        assertThat(listCode).isZero();
        assertThat(outSink.toString()).doesNotContain(id);
    }
}
