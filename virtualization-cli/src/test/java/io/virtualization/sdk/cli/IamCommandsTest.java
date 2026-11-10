package io.virtualization.sdk.cli;

import io.virtualization.sdk.cli.support.FakeImageProvider;
import io.virtualization.sdk.cli.support.FakeVirtualizationProvider;
import io.virtualization.sdk.core.ProviderRegistry;
import io.virtualization.sdk.core.VirtualizationClient;
import io.virtualization.sdk.core.image.ImageProviderRegistry;
import io.virtualization.sdk.iam.DefaultApiCredentialManager;
import io.virtualization.sdk.iam.PasswordHasher;
import io.virtualization.sdk.iam.Permission;
import io.virtualization.sdk.iam.Role;
import io.virtualization.sdk.iam.RoleId;
import io.virtualization.sdk.iam.User;
import io.virtualization.sdk.iam.UserId;
import io.virtualization.sdk.iam.store.JsonFileIamStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves permission enforcement: off by default (every other {@code *CommandsTest} in this module
 * already covers that implicitly — they never pass {@code --api-token} and still pass); with
 * {@code virtualization.iam.enabled: true}, a missing token and an under-permissioned token both
 * exit {@link ExitCodes#AUTH_ERROR}, a correctly-permissioned one succeeds, and the very first
 * enforcement-gated invocation against an empty IAM store bootstraps a default admin and prints its
 * one-time password to stderr. Tokens are minted directly against the same {@link
 * JsonFileIamStore} file the CLI itself reads — there's no {@code iam} CLI command yet to mint one
 * end-to-end (out of scope for this increment) — proving the CLI's real {@code
 * AbstractCliCommand#authenticatedUser()} code path actually reads what a separate writer persisted
 * to the shared file, the same cross-invocation-persistence property {@code VpsCommandsTest} proves
 * for VPS state.
 */
class IamCommandsTest {

    private Path iamStatePath;
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
        Files.writeString(configPath, "virtualization:\n  iam:\n    enabled: true\n");
        configFlag = configPath.toString();
        iamStatePath = tempDir.resolve("iam.json");
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

    private String mintToken(String... permissions) {
        JsonFileIamStore store = new JsonFileIamStore(iamStatePath);
        Set<Permission> perms = Set.of(permissions).stream().map(Permission::parse).collect(Collectors.toSet());
        Role role = new Role(RoleId.generate(), "role-" + UUID.randomUUID(), perms, Instant.now());
        store.roles().save(role);
        User user = new User(
                UserId.generate(), "user-" + UUID.randomUUID(), PasswordHasher.hash("unused".toCharArray()), Set.of(role.id()), Set.of(),
                true, Instant.now());
        store.users().save(user);
        return new DefaultApiCredentialManager(store.apiCredentials(), store.users()).issue(user.id(), "test-token", null).rawToken();
    }

    @Test
    void missingApiTokenExitsAuthErrorAndBootstrapsTheDefaultAdmin() {
        int code = execute("vps", "list", "--provider", "test", "--config", configFlag);

        assertThat(code).isEqualTo(ExitCodes.AUTH_ERROR);
        assertThat(errSink.toString()).contains("Error:");
        assertThat(errSink.toString()).contains("Created default admin user 'admin'.").contains("Password:").contains("API token:");
    }

    @Test
    void aCorrectlyPermissionedTokenSucceeds() {
        String token = mintToken("vps:list");

        int code = execute("vps", "list", "--provider", "test", "--config", configFlag, "--api-token", token, "--output", "json");

        assertThat(code).isZero();
    }

    @Test
    void anUnderPermissionedTokenExitsAuthError() {
        String token = mintToken("vps:get");

        int code = execute("vps", "list", "--provider", "test", "--config", configFlag, "--api-token", token);

        assertThat(code).isEqualTo(ExitCodes.AUTH_ERROR);
        assertThat(errSink.toString()).contains("Missing required permission 'vps:list'");
    }

    @Test
    void enforcementIsOffByDefaultWithoutTheIamEnabledFlag(@TempDir Path anotherDir) throws Exception {
        VirtualizationClient client = new VirtualizationClient(
                new ProviderRegistry(Map.of("test", new FakeVirtualizationProvider())),
                new ImageProviderRegistry(Map.of("test", new FakeImageProvider())));
        Path plainConfig = anotherDir.resolve("config.yaml");
        Files.writeString(plainConfig, "virtualization: {}\n");
        StringWriter plainErr = new StringWriter();
        CommandLine plainCommandLine = new CommandLine(new VirtualizationCli(path -> client))
                .setExecutionExceptionHandler(new CliExceptionHandler())
                .setCaseInsensitiveEnumValuesAllowed(true)
                .setOut(new PrintWriter(new StringWriter()))
                .setErr(new PrintWriter(plainErr));

        int code = plainCommandLine.execute("vps", "list", "--provider", "test", "--config", plainConfig.toString());

        assertThat(code).as(plainErr.toString()).isZero();
    }
}
