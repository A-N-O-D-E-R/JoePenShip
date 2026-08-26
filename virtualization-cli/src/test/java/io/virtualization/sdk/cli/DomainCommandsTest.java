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

/**
 * Documents a known, spec-inherited gap (not a bug introduced by this phase): nothing anywhere —
 * not the spec's own CLI command list (§35), not the REST layer (Phase 8) — ever calls {@code
 * DomainManager.register}, so a fresh {@code InMemoryDomainRepository} per CLI invocation is
 * exactly as populated as Spring's own singleton one: never. {@code domain list} is therefore
 * always empty and {@code domain get} always 404s, for any input.
 */
class DomainCommandsTest {

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
    void listIsAlwaysEmpty() {
        int code = execute("domain", "list", "--config", configFlag, "--output", "json");

        assertThat(code).isZero();
        assertThat(outSink.toString().replaceAll("\\s+", "")).isEqualTo("[]");
    }

    @Test
    void getAnyDomainReturnsResourceNotFoundExitCode() {
        int code = execute("domain", "get", "example.com", "--config", configFlag);

        assertThat(code).isEqualTo(ExitCodes.RESOURCE_NOT_FOUND);
        assertThat(errSink.toString()).contains("Error:");
    }
}
