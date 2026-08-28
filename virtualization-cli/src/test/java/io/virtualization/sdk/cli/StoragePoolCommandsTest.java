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

class StoragePoolCommandsTest {

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
    void listReturnsTheSeededDefaultPool() {
        int code = execute("storage-pool", "list", "--provider", "test", "--output", "json");

        assertThat(code).isZero();
        assertThat(outSink.toString()).contains("default").contains("dir");
    }

    @Test
    void getReturnsTheNamedPool() {
        int code = execute("storage-pool", "get", "default", "--provider", "test", "--output", "json");

        assertThat(code).isZero();
        assertThat(outSink.toString()).contains("Created");
    }

    @Test
    void getUnknownPoolReturnsResourceNotFoundExitCode() {
        int code = execute("storage-pool", "get", "missing", "--provider", "test");

        assertThat(code).isEqualTo(ExitCodes.RESOURCE_NOT_FOUND);
    }
}
