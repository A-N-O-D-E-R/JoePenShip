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
 * Proves the {@code iam user/role/group/credential} command tree end to end, across separate
 * {@code execute()} calls (the CLI-persistence property that matters — see {@code
 * VpsCommandsTest}): create a role, grant it a permission, create a user, assign the role, issue
 * an API credential, and confirm that credential actually authenticates a *later* enforcement-gated
 * command with exactly the granted permission — this is the loop the previous increment's "Known
 * limitations" note flagged as missing (no CLI/REST surface to create a second user).
 */
class IamManagementCommandsTest {

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

    /** The very first enforcement-gated command against an empty store bootstraps its own admin API token. */
    private String adminToken() {
        execute("iam", "role", "list", "--config", configFlag, "--api-token", "bogus-to-force-bootstrap-attempt");
        Matcher matcher = Pattern.compile("API token: (\\S+)").matcher(errSink.toString());
        assertThat(matcher.find()).as("bootstrap API token in: " + errSink).isTrue();
        return matcher.group(1);
    }

    @Test
    void createRoleGrantPermissionCreateUserAssignRoleAndIssueCredentialEndToEnd() {
        String admin = adminToken();

        int createRole = execute(
                "iam", "role", "create", "--name", "operators", "--config", configFlag, "--api-token", admin, "--output", "json");
        assertThat(createRole).isZero();
        String roleId = extractField(outSink.toString(), "id");

        int grant = execute(
                "iam", "role", "grant-permission", roleId, "--permission", "vps:list", "--config", configFlag, "--api-token", admin,
                "--output", "json");
        assertThat(grant).isZero();
        assertThat(outSink.toString()).contains("vps:list");

        int createUser = execute(
                "iam", "user", "create", "--username", "alice", "--password", "s3cret!", "--config", configFlag, "--api-token", admin,
                "--output", "json");
        assertThat(createUser).isZero();
        assertThat(outSink.toString()).doesNotContain("s3cret").doesNotContain("passwordHash");
        String userId = extractField(outSink.toString(), "id");

        int assignRole = execute(
                "iam", "user", "assign-role", userId, roleId, "--config", configFlag, "--api-token", admin, "--output", "json");
        assertThat(assignRole).isZero();
        assertThat(outSink.toString()).contains(roleId);

        int issue = execute(
                "iam", "credential", "issue", "--owner-id", userId, "--name", "alice-token", "--config", configFlag, "--api-token", admin,
                "--output", "json");
        assertThat(issue).isZero();
        String aliceToken = extractField(outSink.toString(), "token");
        assertThat(aliceToken).startsWith("vsdk_");

        // Alice's freshly issued token, minted in one execute() call, authenticates successfully
        // in a *separate* execute() call — proving cross-invocation persistence through the same
        // iam.json every prior command in this test also used.
        int vpsListAsAlice = execute("vps", "list", "--provider", "test", "--config", configFlag, "--api-token", aliceToken);
        assertThat(vpsListAsAlice).isZero();

        int vpsCreateAsAlice = execute(
                "vps", "create", "--provider", "test", "--image", "images:ubuntu/24.04", "--name", "web-01", "--config", configFlag,
                "--api-token", aliceToken);
        assertThat(vpsCreateAsAlice).isEqualTo(ExitCodes.AUTH_ERROR);

        int disable = execute("iam", "user", "disable", userId, "--config", configFlag, "--api-token", admin);
        assertThat(disable).isZero();

        int vpsListAfterDisable = execute("vps", "list", "--provider", "test", "--config", configFlag, "--api-token", aliceToken);
        assertThat(vpsListAfterDisable).isEqualTo(ExitCodes.AUTH_ERROR);
    }

    @Test
    void groupMembershipGrantsPermissionsThroughItsRoles() {
        String admin = adminToken();

        int createRole = execute(
                "iam", "role", "create", "--name", "ops-role", "--config", configFlag, "--api-token", admin, "--output", "json");
        String roleId = extractField(outSink.toString(), "id");
        execute("iam", "role", "grant-permission", roleId, "--permission", "vps:get", "--config", configFlag, "--api-token", admin);

        int createGroup = execute(
                "iam", "group", "create", "--name", "ops-team", "--config", configFlag, "--api-token", admin, "--output", "json");
        String groupId = extractField(outSink.toString(), "id");
        execute("iam", "group", "assign-role", groupId, roleId, "--config", configFlag, "--api-token", admin);

        int createUser = execute(
                "iam", "user", "create", "--username", "bob", "--password", "s3cret!", "--config", configFlag, "--api-token", admin,
                "--output", "json");
        String userId = extractField(outSink.toString(), "id");
        int addMember = execute("iam", "group", "add-member", groupId, userId, "--config", configFlag, "--api-token", admin, "--output", "json");
        assertThat(addMember).isZero();
        assertThat(outSink.toString()).contains(userId);

        int issue = execute(
                "iam", "credential", "issue", "--owner-id", userId, "--name", "bob-token", "--config", configFlag, "--api-token", admin,
                "--output", "json");
        String bobToken = extractField(outSink.toString(), "token");

        // bob has no directly assigned role — vps:get comes only through his group's role.
        int bobGet = execute("vps", "get", "some-id", "--provider", "test", "--config", configFlag, "--api-token", bobToken);
        assertThat(bobGet).isEqualTo(ExitCodes.RESOURCE_NOT_FOUND); // security let it through; the id itself is unknown

        int bobList = execute("vps", "list", "--provider", "test", "--config", configFlag, "--api-token", bobToken);
        assertThat(bobList).isEqualTo(ExitCodes.AUTH_ERROR);
    }

    private static String extractField(String json, String field) {
        Matcher matcher = Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        assertThat(matcher.find()).as(field + " in: " + json).isTrue();
        return matcher.group(1);
    }
}
