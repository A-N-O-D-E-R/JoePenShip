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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves certificate state survives across separate CLI invocations (via {@code
 * JsonFileCertificateRepository}/{@code EncryptedFileCertificateStore}) and, specifically, that
 * {@code renew}/{@code revoke} on a certificate requested in an *earlier* invocation now succeed —
 * this is the blast-radius proof for the {@code AcmeProvider.renew}/{@code .revoke} upstream fix
 * (was keyed by {@code CertificateId} against a provider's private in-process map, which a fresh
 * process could never populate). Mirrors {@code VpsCommandsTest}'s cross-invocation pattern.
 */
class CertificateCommandsTest {

    private static final String PASSPHRASE = "correct horse battery staple";

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
                  certificates:
                    providers:
                      letsencrypt:
                        type: mock
                        dns-provider: cloudflare
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

    private String requestCertificate() {
        int code = execute(
                "certificate", "request", "--domain", "example.com", "--provider", "letsencrypt", "--config", configFlag,
                "--passphrase", PASSPHRASE, "--output", "json");
        assertThat(code).isZero();
        return extractCertificateId(outSink.toString());
    }

    @Test
    void requestThenGetAndListSurviveAcrossInvocations() {
        String id = requestCertificate();

        int getCode = execute("certificate", "get", id, "--config", configFlag, "--output", "json");
        assertThat(getCode).isZero();
        assertThat(outSink.toString()).contains(id).contains("\"status\" : \"ACTIVE\"");

        int listCode = execute("certificate", "list", "--config", configFlag, "--output", "json");
        assertThat(listCode).isZero();
        assertThat(outSink.toString()).contains(id);
    }

    @Test
    void listAndGetSucceedWithoutAnyPassphrase() {
        String id = requestCertificate();

        int listCode = execute("certificate", "list", "--config", configFlag, "--output", "json");
        assertThat(listCode).isZero();

        int getCode = execute("certificate", "get", id, "--config", configFlag, "--output", "json");
        assertThat(getCode).isZero();
    }

    @Test
    void renewOnACertificateFromAnEarlierInvocationSucceeds() {
        String id = requestCertificate();

        int renewCode = execute("certificate", "renew", id, "--config", configFlag, "--output", "json");

        assertThat(renewCode).isZero();
        assertThat(outSink.toString()).contains(id).contains("\"status\" : \"ACTIVE\"");
    }

    @Test
    void revokeOnACertificateFromAnEarlierInvocationSucceeds() {
        String id = requestCertificate();

        int revokeCode = execute("certificate", "revoke", id, "--config", configFlag, "--output", "json");
        assertThat(revokeCode).isZero();

        int getCode = execute("certificate", "get", id, "--config", configFlag, "--output", "json");
        assertThat(getCode).isZero();
        assertThat(outSink.toString()).contains("\"status\" : \"REVOKED\"");
    }

    @Test
    void exportWithIncludePrivateKeyAndYesRoundTripsAllMaterial(@TempDir Path exportDir) throws Exception {
        String id = requestCertificate();
        Path certPath = exportDir.resolve("cert.pem");
        Path chainPath = exportDir.resolve("chain.pem");
        Path keyPath = exportDir.resolve("key.pem");

        int code = execute(
                "certificate", "export", id, "--config", configFlag, "--passphrase", PASSPHRASE,
                "--cert", certPath.toString(), "--chain", chainPath.toString(),
                "--private-key", keyPath.toString(), "--include-private-key", "--yes");

        assertThat(code).isZero();
        assertThat(certPath).exists();
        assertThat(chainPath).exists();
        assertThat(keyPath).exists();
        assertThat(Files.readString(keyPath, StandardCharsets.UTF_8)).contains("BEGIN PRIVATE KEY");
    }

    @Test
    void exportWithPrivateKeyPathButMissingConfirmationFlagsRefuses(@TempDir Path exportDir) {
        String id = requestCertificate();
        Path keyPath = exportDir.resolve("key.pem");

        int code = execute(
                "certificate", "export", id, "--config", configFlag, "--passphrase", PASSPHRASE,
                "--private-key", keyPath.toString());

        assertThat(code).isEqualTo(ExitCodes.USAGE);
        assertThat(keyPath).doesNotExist();
    }

    /** {@code Certificate.id()} is a {@code CertificateId} record — serializes as {@code "id" : { "value" : "cert-..." } }, not a bare string. */
    private static String extractCertificateId(String json) {
        Matcher matcher = Pattern.compile("\"id\"\\s*:\\s*\\{\\s*\"value\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        assertThat(matcher.find()).as("id in: " + json).isTrue();
        return matcher.group(1);
    }
}
