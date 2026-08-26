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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The CLI process doesn't stay alive between invocations — {@code --config <path>} pins a
 * per-test {@code @TempDir} config file (with a {@code cloudflare} mock DNS provider configured
 * for zone {@code example.com}) so state written by one {@code execute()} call is actually
 * readable by the next, proving {@code JsonFileDnsProvider} bridges separate CLI runs. Mirrors
 * {@code VpsCommandsTest}'s cross-invocation proof.
 */
class DnsCommandsTest {

    private String configFlag;
    private StringWriter outSink;
    private StringWriter errSink;
    private CommandLine commandLine;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws Exception {
        VirtualizationClient client = new VirtualizationClient(
                new ProviderRegistry(Map.of("test", new FakeVirtualizationProvider())),
                new ImageProviderRegistry(Map.of("test", new FakeImageProvider())));
        Path configPath = tempDir.resolve("config.yaml");
        Files.writeString(configPath, """
                virtualization:
                  dns:
                    providers:
                      cloudflare:
                        type: mock
                        zones: [example.com]
                """);
        configFlag = configPath.toString();
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
    void zoneListReturnsTheConfiguredZone() {
        int code = execute("dns", "zone", "list", "--dns-provider", "cloudflare", "--config", configFlag, "--output", "json");

        assertThat(code).isZero();
        assertThat(outSink.toString()).contains("example.com");
    }

    @Test
    void recordCreatedInOneInvocationIsVisibleInTheNext() {
        int createCode = execute(
                "dns", "record", "create", "example.com", "--dns-provider", "cloudflare", "--config", configFlag,
                "--name", "app", "--type", "A", "--value", "203.0.113.10", "--output", "json");
        assertThat(createCode).isZero();
        assertThat(outSink.toString()).contains("\"name\" : \"app\"");

        int listCode = execute("dns", "record", "list", "example.com", "--dns-provider", "cloudflare", "--config", configFlag, "--output", "json");
        assertThat(listCode).isZero();
        assertThat(outSink.toString()).contains("app").contains("203.0.113.10");
    }

    @Test
    void deleteRemovesTheRecordFromSubsequentListings() {
        execute(
                "dns", "record", "create", "example.com", "--dns-provider", "cloudflare", "--config", configFlag,
                "--name", "app", "--type", "A", "--value", "203.0.113.10", "--output", "json");
        String recordId = extractField(outSink.toString(), "id");

        int deleteCode = execute("dns", "record", "delete", "example.com", recordId, "--dns-provider", "cloudflare", "--config", configFlag);
        assertThat(deleteCode).isZero();

        int listCode = execute("dns", "record", "list", "example.com", "--dns-provider", "cloudflare", "--config", configFlag, "--output", "json");
        assertThat(listCode).isZero();
        assertThat(outSink.toString()).doesNotContain(recordId);
    }

    @Test
    void createOnAnUnknownZoneReturnsResourceNotFoundExitCode() {
        int code = execute(
                "dns", "record", "create", "unknown.com", "--dns-provider", "cloudflare", "--config", configFlag,
                "--name", "app", "--type", "A", "--value", "203.0.113.10");

        assertThat(code).isEqualTo(ExitCodes.RESOURCE_NOT_FOUND);
    }

    private static String extractField(String json, String field) {
        Matcher matcher = Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        assertThat(matcher.find()).as(field + " in: " + json).isTrue();
        return matcher.group(1);
    }
}
