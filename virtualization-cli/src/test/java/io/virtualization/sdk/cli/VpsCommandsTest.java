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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The CLI process doesn't stay alive between invocations — {@code --config <path>} pins
 * {@link VirtualizationCli#vpsStatePath()} to a per-test {@code @TempDir} so state written by one
 * {@code execute()} call is actually readable by the next, proving {@code JsonFileVpsRepository}
 * bridges separate CLI runs (not just in-JVM object reuse).
 */
class VpsCommandsTest {

    private static final Pattern VPS_ID = Pattern.compile("\"vpsId\"\\s*:\\s*\"([^\"]+)\"");

    private FakeVirtualizationProvider fakeProvider;
    private String configFlag;
    private StringWriter outSink;
    private StringWriter errSink;
    private CommandLine commandLine;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        fakeProvider = new FakeVirtualizationProvider();
        VirtualizationClient client = new VirtualizationClient(
                new ProviderRegistry(Map.of("test", fakeProvider)), new ImageProviderRegistry(Map.of("test", new FakeImageProvider())));
        configFlag = tempDir.resolve("config.yaml").toString();
        outSink = new StringWriter();
        errSink = new StringWriter();
        commandLine = new CommandLine(new VirtualizationCli(configPath -> client))
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

    private String createVps(String name) {
        int code = execute(
                "vps", "create", "--provider", "test", "--config", configFlag, "--image", "images:ubuntu/24.04",
                "--name", name, "--output", "json");
        assertThat(code).isZero();
        Matcher matcher = VPS_ID.matcher(outSink.toString());
        assertThat(matcher.find()).as("vpsId in: " + outSink).isTrue();
        return matcher.group(1);
    }

    @Test
    void createSucceedsAndPrintsVpsId() {
        int code = execute(
                "vps", "create", "--provider", "test", "--config", configFlag, "--image", "images:ubuntu/24.04",
                "--name", "web-01", "--output", "json");

        assertThat(code).isZero();
        assertThat(outSink.toString()).contains("SUCCEEDED").contains("\"vpsId\"");
    }

    @Test
    void createFailureForUnknownImageReturnsGeneralErrorExitCode() {
        int code = execute(
                "vps", "create", "--provider", "test", "--config", configFlag, "--image", "images:missing",
                "--name", "web-01", "--output", "json");

        assertThat(code).isEqualTo(ExitCodes.GENERAL_ERROR);
        assertThat(outSink.toString()).contains("FAILED");
    }

    @Test
    void listAndGetSeeAVpsCreatedInAnEarlierInvocation() {
        String id = createVps("web-01");

        int listCode = execute("vps", "list", "--provider", "test", "--config", configFlag, "--output", "json");
        assertThat(listCode).isZero();
        assertThat(outSink.toString()).contains("web-01").contains(id);

        int getCode = execute("vps", "get", id, "--provider", "test", "--config", configFlag, "--output", "json");
        assertThat(getCode).isZero();
        assertThat(outSink.toString()).contains("\"state\" : \"READY\"").contains("web-01");
    }

    @Test
    void getUnknownVpsReturnsResourceNotFoundExitCode() {
        int code = execute("vps", "get", "vps-does-not-exist", "--provider", "test", "--config", configFlag);

        assertThat(code).isEqualTo(ExitCodes.RESOURCE_NOT_FOUND);
        assertThat(errSink.toString()).contains("Error:");
    }

    @Test
    void lifecycleTransitionsPersistAcrossInvocations() {
        String id = createVps("web-01");

        int stopCode = execute("vps", "stop", id, "--provider", "test", "--config", configFlag, "--output", "json");
        assertThat(stopCode).isZero();
        assertThat(outSink.toString()).contains("SUCCEEDED");

        int getCode = execute("vps", "get", id, "--provider", "test", "--config", configFlag, "--output", "json");
        assertThat(getCode).isZero();
        assertThat(outSink.toString()).contains("\"state\" : \"STOPPED\"");

        int startCode = execute("vps", "start", id, "--provider", "test", "--config", configFlag, "--output", "json");
        assertThat(startCode).isZero();

        int getCode2 = execute("vps", "get", id, "--provider", "test", "--config", configFlag, "--output", "json");
        assertThat(outSink.toString()).contains("\"state\" : \"RUNNING\"");
        assertThat(getCode2).isZero();
    }

    @Test
    void invalidLifecycleTransitionReturnsGeneralErrorExitCode() {
        String id = createVps("web-01");

        // freshly created VPS is READY, not STOPPED — start() only accepts STOPPED.
        int code = execute("vps", "start", id, "--provider", "test", "--config", configFlag);

        assertThat(code).isEqualTo(ExitCodes.GENERAL_ERROR);
        assertThat(errSink.toString()).contains("Error:");
    }

    @Test
    void destroyRemovesTheVpsFromSubsequentListings() {
        String id = createVps("web-01");

        int destroyCode = execute("vps", "destroy", id, "--provider", "test", "--config", configFlag, "--output", "json");
        assertThat(destroyCode).isZero();

        int getCode = execute("vps", "get", id, "--provider", "test", "--config", configFlag, "--output", "json");
        assertThat(getCode).isZero();
        assertThat(outSink.toString()).contains("\"state\" : \"DESTROYED\"");
    }

    @Test
    void rebuildUpdatesImageAndReturnsToReady() {
        String id = createVps("web-01");

        int code = execute(
                "vps", "rebuild", id, "--provider", "test", "--config", configFlag, "--image", "images:ubuntu/24.04",
                "--output", "json");

        assertThat(code).isZero();
        assertThat(outSink.toString()).contains("SUCCEEDED");
    }
}
