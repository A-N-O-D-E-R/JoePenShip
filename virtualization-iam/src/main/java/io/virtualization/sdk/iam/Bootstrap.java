package io.virtualization.sdk.iam;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Creates a default {@code admin} user/role, granted every {@link Permissions#ALL} permission, the
 * first time an IAM store is found completely empty — a fresh install has no other way in. Pure
 * logic, no I/O: callers (the CLI, {@code IamAutoConfiguration}) own deciding how to surface the
 * returned one-time credentials (never persisted in the clear, never returned again after this
 * call).
 *
 * <p>Also mints an initial {@link ApiCredential} for the new admin, not just a password: neither
 * the CLI (its {@code --api-token} flag) nor the REST layer (its {@code Authorization: Bearer}
 * filter) has any username/password login path wired up — a bare password would leave the freshly
 * bootstrapped admin with no actual way in.
 */
public final class Bootstrap {

    public static final String DEFAULT_ADMIN_USERNAME = "admin";
    public static final String DEFAULT_ADMIN_ROLE = "admin";
    private static final String BOOTSTRAP_CREDENTIAL_NAME = "bootstrap";

    private Bootstrap() {}

    /** The admin's one-time password and API token — both usable exactly once, from this call. */
    public record Result(String password, String apiToken) {}

    /** @return the freshly generated one-time admin credentials, or {@link Optional#empty()} if the store already had at least one user */
    public static Optional<Result> ensureAdmin(UserRepository users, RoleRepository roles, ApiCredentialRepository apiCredentials) {
        if (!users.findAll().isEmpty()) {
            return Optional.empty();
        }
        Set<Permission> allPermissions = Permissions.ALL.stream().map(Permission::parse).collect(Collectors.toSet());
        Role adminRole = new Role(RoleId.generate(), DEFAULT_ADMIN_ROLE, allPermissions, Instant.now());
        roles.save(adminRole);

        String rawPassword = ApiTokens.generate();
        User admin = new User(
                UserId.generate(), DEFAULT_ADMIN_USERNAME, PasswordHasher.hash(rawPassword.toCharArray()),
                Set.of(adminRole.id()), Set.of(), true, Instant.now());
        users.save(admin);

        String rawToken = ApiTokens.generate();
        Instant now = Instant.now();
        apiCredentials.save(new ApiCredential(ApiCredentialId.generate(), admin.id(), BOOTSTRAP_CREDENTIAL_NAME, ApiTokens.hash(rawToken), now, null, null));

        return Optional.of(new Result(rawPassword, rawToken));
    }
}
