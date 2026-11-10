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

/** Cross-invocation proof, exact {@code ProjectCommandsTest} shape — {@code JsonFileNetworkStore} bridges separate CLI runs. */
class NetworkCommandsTest {

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

    @Test
    void createSucceedsAndPrintsANetworkId() {
        int code = execute("network", "create", "--name", "public", "--description", "public-facing", "--config", configFlag, "--output", "json");

        assertThat(code).isZero();
        assertThat(outSink.toString()).contains("public").contains("public-facing");
    }

    @Test
    void createRejectsADuplicateName() {
        execute("network", "create", "--name", "public", "--config", configFlag);

        int code = execute("network", "create", "--name", "public", "--config", configFlag);

        assertThat(code).isEqualTo(ExitCodes.GENERAL_ERROR);
    }

    @Test
    void listAndGetSeeANetworkCreatedInAnEarlierInvocation() {
        execute("network", "create", "--name", "public", "--config", configFlag, "--output", "json");
        String id = extractId(outSink.toString());

        int listCode = execute("network", "list", "--config", configFlag, "--output", "json");
        assertThat(listCode).isZero();
        assertThat(outSink.toString()).contains("public").contains(id);

        int getCode = execute("network", "get", id, "--config", configFlag, "--output", "json");
        assertThat(getCode).isZero();
        assertThat(outSink.toString()).contains("public");
    }

    @Test
    void getUnknownNetworkReturnsResourceNotFoundExitCode() {
        int code = execute("network", "get", "network-does-not-exist", "--config", configFlag);

        assertThat(code).isEqualTo(ExitCodes.RESOURCE_NOT_FOUND);
    }

    @Test
    void deleteRemovesTheNetworkFromSubsequentListings() {
        execute("network", "create", "--name", "public", "--config", configFlag, "--output", "json");
        String id = extractId(outSink.toString());

        int deleteCode = execute("network", "delete", id, "--config", configFlag);
        assertThat(deleteCode).isZero();

        int listCode = execute("network", "list", "--config", configFlag, "--output", "json");
        assertThat(listCode).isZero();
        assertThat(outSink.toString()).doesNotContain(id);
    }

    /** {@code Network.id()} is a {@code NetworkId} record — serializes as {@code "id" : { "value" : "network-..." } }, not a bare string. */
    static String extractId(String json) {
        Matcher matcher = Pattern.compile("\"id\"\\s*:\\s*\\{\\s*\"value\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        assertThat(matcher.find()).as("id in: " + json).isTrue();
        return matcher.group(1);
    }
}
