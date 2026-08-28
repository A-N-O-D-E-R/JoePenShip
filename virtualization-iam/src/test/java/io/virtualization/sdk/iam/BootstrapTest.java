package io.virtualization.sdk.iam;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BootstrapTest {

    @Test
    void createsAnAdminUserRoleAndApiCredentialWhenTheStoreIsEmpty() {
        UserRepository users = new InMemoryUserRepository();
        RoleRepository roles = new InMemoryRoleRepository();
        ApiCredentialRepository apiCredentials = new InMemoryApiCredentialRepository();

        Optional<Bootstrap.Result> result = Bootstrap.ensureAdmin(users, roles, apiCredentials);

        assertThat(result).isPresent();
        User admin = users.findByUsername(Bootstrap.DEFAULT_ADMIN_USERNAME).orElseThrow();
        assertThat(admin.enabled()).isTrue();
        assertThat(PasswordHasher.verify(result.get().password().toCharArray(), admin.passwordHash())).isTrue();

        Role adminRole = roles.findByName(Bootstrap.DEFAULT_ADMIN_ROLE).orElseThrow();
        assertThat(admin.roles()).containsExactly(adminRole.id());
        assertThat(adminRole.permissions()).hasSize(Permissions.ALL.size());

        ApiCredential credential = apiCredentials.findByOwner(admin.id()).stream().findFirst().orElseThrow();
        assertThat(credential.tokenHash()).isEqualTo(ApiTokens.hash(result.get().apiToken()));
    }

    @Test
    void doesNothingWhenTheStoreAlreadyHasAUser() {
        UserRepository users = new InMemoryUserRepository();
        RoleRepository roles = new InMemoryRoleRepository();
        ApiCredentialRepository apiCredentials = new InMemoryApiCredentialRepository();
        users.save(new User(UserId.generate(), "existing", "pbkdf2-sha256$1$AA$AA", Set.of(), Set.of(), true, Instant.now()));

        Optional<Bootstrap.Result> result = Bootstrap.ensureAdmin(users, roles, apiCredentials);

        assertThat(result).isEmpty();
        assertThat(users.findByUsername(Bootstrap.DEFAULT_ADMIN_USERNAME)).isEmpty();
        assertThat(roles.findAll()).isEmpty();
        assertThat(apiCredentials.findAll()).isEmpty();
    }
}
