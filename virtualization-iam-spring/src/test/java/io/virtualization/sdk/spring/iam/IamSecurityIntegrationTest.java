package io.virtualization.sdk.spring.iam;

import io.virtualization.sdk.iam.ApiCredentialManager;
import io.virtualization.sdk.iam.PasswordHasher;
import io.virtualization.sdk.iam.Permission;
import io.virtualization.sdk.iam.Role;
import io.virtualization.sdk.iam.RoleId;
import io.virtualization.sdk.iam.RoleRepository;
import io.virtualization.sdk.iam.User;
import io.virtualization.sdk.iam.UserId;
import io.virtualization.sdk.iam.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fires real HTTP requests at a real embedded server through the actual {@code
 * SecurityFilterChain} — {@code /api/v1/vps} has no real controller registered in this minimal
 * {@code TestApp} (this module never depends on {@code virtualization-spring-boot-autoconfigure}),
 * so a request that gets past security lands on a real {@code 404}, not {@code 200}; the point is
 * distinguishing that from the {@code 401}/{@code 403} security itself produces before dispatch
 * ever happens. {@code /api/v1/probe} is a real endpoint, used to prove the {@code anyRequest()
 * .authenticated()} catch-all for paths outside {@link PermissionRequestMatchers}' fixed catalog.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = IamSecurityIntegrationTest.TestApp.class,
        properties = "virtualization.iam.enabled=true")
class IamSecurityIntegrationTest {

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

    private HttpResponse<String> get(String path, String bearerToken) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
        if (bearerToken != null) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String mintToken(String... permissions) {
        Set<Permission> perms = Set.of(permissions).stream().map(Permission::parse).collect(Collectors.toSet());
        Role role = new Role(RoleId.generate(), "role-" + UUID.randomUUID(), perms, Instant.now());
        roles.save(role);
        User user = new User(
                UserId.generate(), "user-" + UUID.randomUUID(), PasswordHasher.hash("unused".toCharArray()), Set.of(role.id()), Set.of(),
                true, Instant.now());
        users.save(user);
        return apiCredentialManager.issue(user.id(), "test-token", null).rawToken();
    }

    @Test
    void noAuthorizationHeaderReturns401() throws Exception {
        assertThat(get("/api/v1/vps", null).statusCode()).isEqualTo(401);
    }

    @Test
    void anUnderPermissionedTokenReturns403() throws Exception {
        String token = mintToken("vps:get");

        assertThat(get("/api/v1/vps", token).statusCode()).isEqualTo(403);
    }

    @Test
    void aCorrectlyPermissionedTokenPassesSecurityAndReaches404NotAuthError() throws Exception {
        String token = mintToken("vps:list");

        assertThat(get("/api/v1/vps", token).statusCode()).isEqualTo(404);
    }

    @Test
    void anyAuthenticatedTokenPassesTheCatchAllForAnUncatalogedPath() throws Exception {
        String token = mintToken("something:unrelated");

        assertThat(get("/api/v1/probe", token).statusCode()).isEqualTo(200);
    }

    @Test
    void anUnauthenticatedRequestToAnUncatalogedPathAlsoReturns401() throws Exception {
        assertThat(get("/api/v1/probe", null).statusCode()).isEqualTo(401);
    }

    @SpringBootApplication
    static class TestApp {

        @RestController
        static class ProbeController {

            @GetMapping("/api/v1/probe")
            String probe() {
                return "ok";
            }
        }
    }
}
