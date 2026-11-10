package io.virtualization.sdk.spring.iam.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.virtualization.sdk.iam.ApiCredentialManager;
import io.virtualization.sdk.iam.PasswordHasher;
import io.virtualization.sdk.iam.Permission;
import io.virtualization.sdk.iam.Permissions;
import io.virtualization.sdk.iam.Role;
import io.virtualization.sdk.iam.RoleId;
import io.virtualization.sdk.iam.RoleRepository;
import io.virtualization.sdk.iam.User;
import io.virtualization.sdk.iam.UserId;
import io.virtualization.sdk.iam.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fires real HTTP requests at the new {@code /api/v1/iam/*} management endpoints — proves the
 * whole loop the previous increment's "Known limitations" note flagged as missing: create a role,
 * grant it a permission, create a user, assign the role, issue that user an API credential, and
 * confirm the freshly issued token actually authenticates with exactly the granted permission (and
 * stops working the moment the user is disabled). A second test proves the same for
 * group-inherited permissions. No real `VpsController` exists in this minimal `TestApp` (this
 * module never depends on {@code virtualization-spring-boot-autoconfigure}), so a request that
 * clears security lands on a real {@code 404}, not {@code 200} — the point is distinguishing that
 * from the {@code 401}/{@code 403} security itself would produce.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = IamManagementIntegrationTest.TestApp.class,
        properties = "virtualization.iam.enabled=true")
class IamManagementIntegrationTest {

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void iamStateFile(DynamicPropertyRegistry registry) {
        registry.add("virtualization.iam.state-file", () -> tempDir.resolve("iam.json").toString());
    }

    @LocalServerPort
    int port;

    @Autowired
    UserRepository users;

    @Autowired
    RoleRepository roles;

    @Autowired
    ApiCredentialManager apiCredentialManager;

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private String adminToken;

    @BeforeEach
    void mintAnAdminToken() {
        Set<Permission> allPermissions = Permissions.ALL.stream().map(Permission::parse).collect(Collectors.toSet());
        Role role = new Role(RoleId.generate(), "test-admin-" + UUID.randomUUID(), allPermissions, Instant.now());
        roles.save(role);
        User user = new User(
                UserId.generate(), "tester-" + UUID.randomUUID(), PasswordHasher.hash("unused".toCharArray()), Set.of(role.id()), Set.of(),
                true, Instant.now());
        users.save(user);
        adminToken = apiCredentialManager.issue(user.id(), "test-admin-token", null).rawToken();
    }

    private HttpResponse<String> withAdmin(String method, String path, Object body) throws Exception {
        return send(method, path, body, adminToken);
    }

    private HttpResponse<String> withToken(String method, String path, String token) throws Exception {
        return send(method, path, null, token);
    }

    private HttpResponse<String> send(String method, String path, Object body, String token) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json");
        HttpRequest.BodyPublisher publisher = body != null
                ? HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))
                : HttpRequest.BodyPublishers.noBody();
        builder.method(method, publisher);
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void createRoleGrantPermissionCreateUserAssignRoleAndIssueCredentialEndToEnd() throws Exception {
        HttpResponse<String> createRole = withAdmin("POST", "/api/v1/iam/roles", new CreateRoleRequestBody("operators"));
        assertThat(createRole.statusCode()).as(createRole.body()).isEqualTo(200);
        String roleId = extractField(createRole.body(), "id");

        HttpResponse<String> grant = withAdmin(
                "POST", "/api/v1/iam/roles/" + roleId + "/permissions/grant", new PermissionRequestBody("vps:list"));
        assertThat(grant.statusCode()).as(grant.body()).isEqualTo(200);
        assertThat(grant.body()).contains("vps:list");

        HttpResponse<String> createUser = withAdmin("POST", "/api/v1/iam/users", new CreateUserRequestBody("alice", "s3cret!"));
        assertThat(createUser.statusCode()).as(createUser.body()).isEqualTo(200);
        assertThat(createUser.body()).doesNotContain("s3cret").doesNotContain("passwordHash");
        String userId = extractField(createUser.body(), "id");

        HttpResponse<String> assignRole = withAdmin("PUT", "/api/v1/iam/users/" + userId + "/roles/" + roleId, null);
        assertThat(assignRole.statusCode()).as(assignRole.body()).isEqualTo(200);
        assertThat(assignRole.body()).contains(roleId);

        HttpResponse<String> issue = withAdmin(
                "POST", "/api/v1/iam/credentials", new IssueApiCredentialRequestBody(userId, "alice-token", null));
        assertThat(issue.statusCode()).as(issue.body()).isEqualTo(200);
        String aliceToken = extractField(issue.body(), "token");
        assertThat(aliceToken).startsWith("vsdk_");

        assertThat(withToken("GET", "/api/v1/vps", aliceToken).statusCode()).isEqualTo(404);

        HttpResponse<String> disable = withAdmin("POST", "/api/v1/iam/users/" + userId + "/disable", null);
        assertThat(disable.statusCode()).as(disable.body()).isEqualTo(200);

        assertThat(withToken("GET", "/api/v1/vps", aliceToken).statusCode()).isEqualTo(401);
    }

    @Test
    void groupMembershipGrantsPermissionsThroughItsRoles() throws Exception {
        HttpResponse<String> createRole = withAdmin("POST", "/api/v1/iam/roles", new CreateRoleRequestBody("ops-role"));
        String roleId = extractField(createRole.body(), "id");
        withAdmin("POST", "/api/v1/iam/roles/" + roleId + "/permissions/grant", new PermissionRequestBody("vps:get"));

        HttpResponse<String> createGroup = withAdmin("POST", "/api/v1/iam/groups", new CreateGroupRequestBody("ops-team"));
        String groupId = extractField(createGroup.body(), "id");
        HttpResponse<String> assignGroupRole = withAdmin("POST", "/api/v1/iam/groups/" + groupId + "/roles/" + roleId, null);
        assertThat(assignGroupRole.statusCode()).as(assignGroupRole.body()).isEqualTo(200);

        HttpResponse<String> createUser = withAdmin("POST", "/api/v1/iam/users", new CreateUserRequestBody("bob", "s3cret!"));
        String userId = extractField(createUser.body(), "id");
        HttpResponse<String> addMember = withAdmin("POST", "/api/v1/iam/groups/" + groupId + "/members/" + userId, null);
        assertThat(addMember.statusCode()).as(addMember.body()).isEqualTo(200);
        assertThat(addMember.body()).contains(userId);

        HttpResponse<String> issue = withAdmin(
                "POST", "/api/v1/iam/credentials", new IssueApiCredentialRequestBody(userId, "bob-token", null));
        String bobToken = extractField(issue.body(), "token");

        // bob has no directly assigned role — vps:get comes only through his group's role.
        assertThat(withToken("GET", "/api/v1/vps/some-id", bobToken).statusCode()).isEqualTo(404);
        assertThat(withToken("GET", "/api/v1/vps", bobToken).statusCode()).isEqualTo(403);
    }

    private static String extractField(String json, String field) {
        Matcher matcher = Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        assertThat(matcher.find()).as(field + " in: " + json).isTrue();
        return matcher.group(1);
    }

    @SpringBootApplication
    static class TestApp {}
}
