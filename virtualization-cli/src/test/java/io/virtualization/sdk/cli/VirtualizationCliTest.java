package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.support.FakeImageProvider;
import io.virtualization.sdk.cli.support.FakeVirtualizationProvider;
import io.virtualization.sdk.core.Capability;
import io.virtualization.sdk.core.ProviderCapabilities;
import io.virtualization.sdk.core.ProviderRegistry;
import io.virtualization.sdk.core.VirtualizationClient;
import io.virtualization.sdk.core.image.ImageProviderRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class VirtualizationCliTest {

    private FakeVirtualizationProvider fakeProvider;
    private FakeImageProvider fakeImageProvider;
    private StringWriter outSink;
    private StringWriter errSink;
    private CommandLine commandLine;

    private void setUp(FakeVirtualizationProvider provider) {
        setUp(provider, new FakeImageProvider());
    }

    private void setUp(FakeVirtualizationProvider provider, FakeImageProvider imageProvider) {
        fakeProvider = provider;
        fakeImageProvider = imageProvider;
        VirtualizationClient client = new VirtualizationClient(
                new ProviderRegistry(Map.of("test", fakeProvider)),
                new ImageProviderRegistry(Map.of("test", fakeImageProvider)));
        outSink = new StringWriter();
        errSink = new StringWriter();
        commandLine = new CommandLine(new VirtualizationCli(configPath -> client))
                .setExecutionExceptionHandler(new CliExceptionHandler())
                .setCaseInsensitiveEnumValuesAllowed(true)
                .setOut(new PrintWriter(outSink))
                .setErr(new PrintWriter(errSink));
    }

    private int execute(String... args) {
        int code = commandLine.execute(args);
        commandLine.getOut().flush();
        commandLine.getErr().flush();
        return code;
    }

    @Test
    void helpPrintsUsageToStdoutWithExitCodeZero() {
        setUp(new FakeVirtualizationProvider());

        int code = execute("--help");

        assertThat(code).isZero();
        assertThat(outSink.toString()).contains("virtualization").contains("Manage virtual machines");
        assertThat(errSink.toString()).isEmpty();
    }

    @Test
    void versionPrintsVersionWithExitCodeZero() {
        setUp(new FakeVirtualizationProvider());

        int code = execute("--version");

        assertThat(code).isZero();
        assertThat(outSink.toString()).contains("virtualization");
    }

    @Test
    void vmListWithGlobalOptionAfterSubcommandPrintsJsonOnStdout() {
        setUp(new FakeVirtualizationProvider());

        int code = execute("vm", "list", "--provider", "test", "--output", "json");

        assertThat(code).isZero();
        assertThat(outSink.toString().strip()).startsWith("[");
        assertThat(outSink.toString()).contains("vm-1");
        assertThat(errSink.toString()).isEmpty();
    }

    @Test
    void vmListWithGlobalOptionBeforeSubcommandAlsoWorks() {
        setUp(new FakeVirtualizationProvider());

        int code = execute("--provider", "test", "vm", "list", "--output", "json");

        assertThat(code).isZero();
        assertThat(outSink.toString()).contains("vm-1");
    }

    @Test
    void vmListWithoutProviderIsAUsageError() {
        setUp(new FakeVirtualizationProvider());

        int code = execute("vm", "list");

        assertThat(code).isEqualTo(ExitCodes.USAGE);
        assertThat(errSink.toString()).contains("--provider");
        assertThat(outSink.toString()).isEmpty();
    }

    @Test
    void vmGetUnknownIdReturnsResourceNotFoundExitCode() {
        setUp(new FakeVirtualizationProvider());

        int code = execute("vm", "get", "does-not-exist", "--provider", "test");

        assertThat(code).isEqualTo(ExitCodes.RESOURCE_NOT_FOUND);
        assertThat(errSink.toString()).contains("Error:");
        assertThat(outSink.toString()).isEmpty();
    }

    @Test
    void vmStartSucceedsAndPrintsOperationResult() {
        setUp(new FakeVirtualizationProvider());

        int code = execute("vm", "start", "vm-1", "--provider", "test", "--output", "json");

        assertThat(code).isZero();
        assertThat(outSink.toString()).contains("SUCCEEDED");
    }

    @Test
    void vmStopFailureReturnsGeneralErrorExitCode() {
        setUp(new FakeVirtualizationProvider());
        fakeProvider.nextOperationFails();

        int code = execute("vm", "stop", "vm-1", "--provider", "test", "--output", "json");

        assertThat(code).isEqualTo(ExitCodes.GENERAL_ERROR);
        assertThat(outSink.toString()).contains("FAILED");
    }

    @Test
    void vmDestroyUnsupportedCapabilityReturnsExpectedExitCode() {
        setUp(new FakeVirtualizationProvider(ProviderCapabilities.of(Capability.START)));

        int code = execute("vm", "destroy", "vm-1", "--provider", "test");

        assertThat(code).isEqualTo(ExitCodes.UNSUPPORTED_CAPABILITY);
        assertThat(errSink.toString()).contains("Error:");
    }

    @Test
    void providerListShowsConfiguredProviders() {
        setUp(new FakeVirtualizationProvider());

        int code = execute("provider", "list", "--output", "table");

        assertThat(code).isZero();
        assertThat(outSink.toString()).contains("test").contains("fake");
    }

    @Test
    void unknownSubcommandIsAUsageError() {
        setUp(new FakeVirtualizationProvider());

        int code = execute("bogus");

        assertThat(code).isEqualTo(ExitCodes.USAGE);
    }

    @Test
    void noWaitFlagParsesAndStillCompletes() {
        setUp(new FakeVirtualizationProvider());

        int code = execute("vm", "start", "vm-1", "--provider", "test", "--no-wait");

        assertThat(code).isZero();
    }

    @Test
    void imageListPrintsConfiguredImage() {
        setUp(new FakeVirtualizationProvider());

        int code = execute("image", "list", "--provider", "test", "--output", "json");

        assertThat(code).isZero();
        assertThat(outSink.toString()).contains("ubuntu/24.04");
    }

    @Test
    void imageSearchFiltersByDistribution() {
        setUp(new FakeVirtualizationProvider());

        int code = execute("image", "search", "--provider", "test", "--distribution", "debian", "--output", "json");

        assertThat(code).isZero();
        assertThat(outSink.toString().replaceAll("\\s+", "")).isEqualTo("[]");
    }

    @Test
    void imageGetResolvesKnownReference() {
        setUp(new FakeVirtualizationProvider());

        int code = execute("image", "get", "images:ubuntu/24.04", "--provider", "test", "--output", "json");

        assertThat(code).isZero();
        assertThat(outSink.toString()).contains("ubuntu/24.04");
    }

    @Test
    void imageGetUnknownReferenceReturnsResourceNotFoundExitCode() {
        setUp(new FakeVirtualizationProvider());

        int code = execute("image", "get", "images:missing", "--provider", "test");

        assertThat(code).isEqualTo(ExitCodes.RESOURCE_NOT_FOUND);
    }

    @Test
    void imagePullSucceedsAndPrintsOperationResult() {
        setUp(new FakeVirtualizationProvider());

        int code = execute("image", "pull", "images:ubuntu/24.04", "--provider", "test", "--output", "json");

        assertThat(code).isZero();
        assertThat(outSink.toString()).contains("SUCCEEDED");
    }

    @Test
    void imagePullFailureReturnsGeneralErrorExitCode() {
        setUp(new FakeVirtualizationProvider());
        fakeImageProvider.nextOperationFails();

        int code = execute("image", "pull", "images:ubuntu/24.04", "--provider", "test", "--output", "json");

        assertThat(code).isEqualTo(ExitCodes.GENERAL_ERROR);
        assertThat(outSink.toString()).contains("FAILED");
    }

    @Test
    void imageDownloadWritesFileAndPrintsChecksum(@TempDir Path tempDir) throws Exception {
        setUp(new FakeVirtualizationProvider());
        Path output = tempDir.resolve("ubuntu.tar.gz");

        int code = execute(
                "image", "download", "images:ubuntu/24.04", "--provider", "test", "--file", output.toString(),
                "--output", "json");

        assertThat(code).isZero();
        assertThat(java.nio.file.Files.readString(output)).isEqualTo("fake-image-bytes");
        assertThat(outSink.toString()).contains("abc123").contains("sha256");
    }

    @Test
    void imageImportSucceedsAndPrintsImportedImage(@TempDir Path tempDir) throws Exception {
        setUp(new FakeVirtualizationProvider());
        Path file = tempDir.resolve("ubuntu.tar.gz");
        java.nio.file.Files.writeString(file, "fake-image-bytes");

        int code = execute("image", "import", file.toString(), "--provider", "test", "--output", "json");

        assertThat(code).isZero();
        assertThat(outSink.toString()).contains("SUCCEEDED").contains("ubuntu/24.04");
    }

    @Test
    void workloadCreateSucceedsAndPrintsWorkloadId() {
        setUp(new FakeVirtualizationProvider());

        int code = execute(
                "workload", "create", "--provider", "test", "--image", "images:ubuntu/24.04", "--name", "ubuntu-test",
                "--output", "json");

        assertThat(code).isZero();
        assertThat(outSink.toString()).contains("ubuntu-test");
    }
}
