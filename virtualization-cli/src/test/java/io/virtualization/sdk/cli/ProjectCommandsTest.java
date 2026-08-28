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
 * The CLI process doesn't stay alive between invocations — {@code --config <path>} pins a
 * per-test {@code @TempDir} config file so state written by one {@code execute()} call is
 * actually readable by the next, proving {@code JsonFileProjectRepository} bridges separate CLI
 * runs. Mirrors {@code VpsCommandsTest}'s/{@code DomainCommandsTest}'s cross-invocation proof.
 */
class ProjectCommandsTest {

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
    void createSucceedsAndPrintsAProjectId() {
        int code = execute("project", "create", "--name", "homelab", "--description", "personal infra", "--config", configFlag, "--output", "json");

        assertThat(code).isZero();
        assertThat(outSink.toString()).contains("homelab").contains("personal infra");
    }

    @Test
    void createRejectsADuplicateName() {
        execute("project", "create", "--name", "homelab", "--config", configFlag);

        int code = execute("project", "create", "--name", "homelab", "--config", configFlag);

        assertThat(code).isEqualTo(ExitCodes.GENERAL_ERROR);
    }

    @Test
    void listAndGetSeeAProjectCreatedInAnEarlierInvocation() {
        execute("project", "create", "--name", "homelab", "--config", configFlag, "--output", "json");
        String id = extractProjectId(outSink.toString());

        int listCode = execute("project", "list", "--config", configFlag, "--output", "json");
        assertThat(listCode).isZero();
        assertThat(outSink.toString()).contains("homelab").contains(id);

        int getCode = execute("project", "get", id, "--config", configFlag, "--output", "json");
        assertThat(getCode).isZero();
        assertThat(outSink.toString()).contains("homelab");
    }

    @Test
    void getUnknownProjectReturnsResourceNotFoundExitCode() {
        int code = execute("project", "get", "project-does-not-exist", "--config", configFlag);

        assertThat(code).isEqualTo(ExitCodes.RESOURCE_NOT_FOUND);
    }

    @Test
    void deleteRemovesTheProjectFromSubsequentListings() {
        execute("project", "create", "--name", "homelab", "--config", configFlag, "--output", "json");
        String id = extractProjectId(outSink.toString());

        int deleteCode = execute("project", "delete", id, "--config", configFlag);
        assertThat(deleteCode).isZero();

        int listCode = execute("project", "list", "--config", configFlag, "--output", "json");
        assertThat(listCode).isZero();
        assertThat(outSink.toString()).doesNotContain(id);
    }

    /** {@code Project.id()} is a {@code ProjectId} record — serializes as {@code "id" : { "value" : "project-..." } }, not a bare string. */
    private static String extractProjectId(String json) {
        Matcher matcher = Pattern.compile("\"id\"\\s*:\\s*\\{\\s*\"value\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        assertThat(matcher.find()).as("id in: " + json).isTrue();
        return matcher.group(1);
    }
}
