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

class VolumeSnapshotCommandsTest {

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
        execute("volume", "create", "data", "--pool", "default", "--provider", "test");
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
    void createSucceedsAndPrintsTheSnapshot() {
        int code = execute(
                "volume", "snapshot", "create", "before-upgrade", "--volume", "data", "--pool", "default", "--provider", "test",
                "--output", "json");

        assertThat(code).isZero();
        assertThat(outSink.toString()).contains("before-upgrade");
    }

    @Test
    void listSeesASnapshotCreatedInAnEarlierInvocation() {
        execute("volume", "snapshot", "create", "before-upgrade", "--volume", "data", "--pool", "default", "--provider", "test");

        int code = execute("volume", "snapshot", "list", "--volume", "data", "--pool", "default", "--provider", "test", "--output", "json");

        assertThat(code).isZero();
        assertThat(outSink.toString()).contains("before-upgrade");
    }

    @Test
    void restoreSucceedsForAnExistingSnapshot() {
        execute("volume", "snapshot", "create", "before-upgrade", "--volume", "data", "--pool", "default", "--provider", "test");

        int code = execute("volume", "snapshot", "restore", "before-upgrade", "--volume", "data", "--pool", "default", "--provider", "test");

        assertThat(code).isZero();
    }

    @Test
    void restoreUnknownSnapshotReturnsResourceNotFoundExitCode() {
        int code = execute("volume", "snapshot", "restore", "missing", "--volume", "data", "--pool", "default", "--provider", "test");

        assertThat(code).isEqualTo(ExitCodes.RESOURCE_NOT_FOUND);
    }

    @Test
    void deleteRemovesTheSnapshotFromSubsequentListings() {
        execute("volume", "snapshot", "create", "before-upgrade", "--volume", "data", "--pool", "default", "--provider", "test");

        int deleteCode = execute("volume", "snapshot", "delete", "before-upgrade", "--volume", "data", "--pool", "default", "--provider", "test");
        assertThat(deleteCode).isZero();

        int listCode = execute("volume", "snapshot", "list", "--volume", "data", "--pool", "default", "--provider", "test", "--output", "json");
        assertThat(listCode).isZero();
        assertThat(outSink.toString()).doesNotContain("before-upgrade");
    }
}
