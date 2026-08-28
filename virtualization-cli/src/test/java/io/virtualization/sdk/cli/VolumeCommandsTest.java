package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.support.FakeImageProvider;
import io.virtualization.sdk.cli.support.FakeStorageProvider;
import io.virtualization.sdk.cli.support.FakeVirtualizationProvider;
import io.virtualization.sdk.core.ProviderRegistry;
import io.virtualization.sdk.core.VirtualizationClient;
import io.virtualization.sdk.core.image.ImageProviderRegistry;
import io.virtualization.sdk.storage.StorageProviderRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class VolumeCommandsTest {

    private StringWriter outSink;
    private StringWriter errSink;
    private CommandLine commandLine;

    @BeforeEach
    void setUp() {
        VirtualizationClient client = new VirtualizationClient(
                new ProviderRegistry(Map.of("test", new FakeVirtualizationProvider())),
                new ImageProviderRegistry(Map.of("test", new FakeImageProvider())));
        StorageProviderRegistry storageProviderRegistry = new StorageProviderRegistry(Map.of("test", new FakeStorageProvider()));
        outSink = new StringWriter();
        errSink = new StringWriter();
        commandLine = new CommandLine(new VirtualizationCli(path -> client, path -> storageProviderRegistry))
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
    void createSucceedsAndPrintsTheVolume() {
        int code = execute(
                "volume", "create", "data", "--pool", "default", "--content-type", "filesystem", "--size", "10GB",
                "--provider", "test", "--output", "json");

        assertThat(code).isZero();
        assertThat(outSink.toString()).contains("data").contains("10GB");
    }

    @Test
    void listAndGetSeeAVolumeCreatedInAnEarlierInvocation() {
        execute("volume", "create", "data", "--pool", "default", "--provider", "test");

        int listCode = execute("volume", "list", "--pool", "default", "--provider", "test", "--output", "json");
        assertThat(listCode).isZero();
        assertThat(outSink.toString()).contains("data");

        int getCode = execute("volume", "get", "data", "--pool", "default", "--provider", "test", "--output", "json");
        assertThat(getCode).isZero();
        assertThat(outSink.toString()).contains("data");
    }

    @Test
    void getUnknownVolumeReturnsResourceNotFoundExitCode() {
        int code = execute("volume", "get", "missing", "--pool", "default", "--provider", "test");

        assertThat(code).isEqualTo(ExitCodes.RESOURCE_NOT_FOUND);
    }

    @Test
    void deleteRemovesTheVolumeFromSubsequentListings() {
        execute("volume", "create", "data", "--pool", "default", "--provider", "test");

        int deleteCode = execute("volume", "delete", "data", "--pool", "default", "--provider", "test");
        assertThat(deleteCode).isZero();

        int listCode = execute("volume", "list", "--pool", "default", "--provider", "test", "--output", "json");
        assertThat(listCode).isZero();
        assertThat(outSink.toString()).doesNotContain("\"name\" : \"data\"");
    }

    @Test
    void resizeChangesTheVolumeSize() {
        execute("volume", "create", "data", "--pool", "default", "--size", "10GB", "--provider", "test");

        int code = execute("volume", "resize", "data", "--pool", "default", "--size", "20GB", "--provider", "test", "--output", "json");

        assertThat(code).isZero();
        assertThat(outSink.toString()).contains("20GB");
    }
}
