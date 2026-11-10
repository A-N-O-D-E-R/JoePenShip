package io.virtualization.sdk.iam.store;

import io.virtualization.sdk.iam.ApiCredential;
import io.virtualization.sdk.iam.ApiCredentialId;
import io.virtualization.sdk.iam.Group;
import io.virtualization.sdk.iam.GroupId;
import io.virtualization.sdk.iam.Permission;
import io.virtualization.sdk.iam.Role;
import io.virtualization.sdk.iam.RoleId;
import io.virtualization.sdk.iam.User;
import io.virtualization.sdk.iam.UserId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.time.Instant;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves real file persistence, not just in-JVM object reuse: writes via one {@link
 * JsonFileIamStore} instance, reads via a *second*, freshly-constructed instance pointed at the
 * same file — mirrors {@code JsonFileVpsRepository}'s own test rationale in {@code
 * virtualization-cli}.
 */
class JsonFileIamStoreTest {

    @Test
    void aUserSavedByOneInstanceIsVisibleToAFreshInstanceOnTheSameFile(@TempDir Path tempDir) {
        Path file = tempDir.resolve("iam.json");
        User user = new User(UserId.generate(), "alice", "pbkdf2-sha256$1$AA$AA", Set.of(), Set.of(), true, Instant.now());

        new JsonFileIamStore(file).users().save(user);

        User reloaded = new JsonFileIamStore(file).users().findById(user.id()).orElseThrow();
        assertThat(reloaded.username()).isEqualTo("alice");
    }

    @Test
    void aRoleSavedByOneInstanceIsVisibleToAFreshInstanceOnTheSameFile(@TempDir Path tempDir) {
        Path file = tempDir.resolve("iam.json");
        Role role = new Role(RoleId.generate(), "operators", Set.of(Permission.parse("vps:create")), Instant.now());

        new JsonFileIamStore(file).roles().save(role);

        Role reloaded = new JsonFileIamStore(file).roles().findByName("operators").orElseThrow();
        assertThat(reloaded.permissions()).containsExactly(Permission.parse("vps:create"));
    }

    @Test
    void aGroupSavedByOneInstanceIsVisibleToAFreshInstanceOnTheSameFile(@TempDir Path tempDir) {
        Path file = tempDir.resolve("iam.json");
        UserId member = UserId.generate();
        Group group = new Group(GroupId.generate(), "ops-team", Set.of(), Set.of(member), Instant.now());

        new JsonFileIamStore(file).groups().save(group);

        Group reloaded = new JsonFileIamStore(file).groups().findByName("ops-team").orElseThrow();
        assertThat(reloaded.members()).containsExactly(member);
    }

    @Test
    void anApiCredentialSavedByOneInstanceIsVisibleToAFreshInstanceOnTheSameFile(@TempDir Path tempDir) {
        Path file = tempDir.resolve("iam.json");
        UserId owner = UserId.generate();
        ApiCredential credential = new ApiCredential(ApiCredentialId.generate(), owner, "ci-token", "deadbeef", Instant.now(), null, null);

        new JsonFileIamStore(file).apiCredentials().save(credential);

        assertThat(new JsonFileIamStore(file).apiCredentials().findByOwner(owner)).containsExactly(credential);
    }

    @Test
    void savingDifferentAggregatesAcrossInstancesDoesNotClobberEachOther(@TempDir Path tempDir) {
        Path file = tempDir.resolve("iam.json");
        User user = new User(UserId.generate(), "alice", "pbkdf2-sha256$1$AA$AA", Set.of(), Set.of(), true, Instant.now());
        Role role = new Role(RoleId.generate(), "operators", Set.of(), Instant.now());

        new JsonFileIamStore(file).users().save(user);
        new JsonFileIamStore(file).roles().save(role);

        JsonFileIamStore reopened = new JsonFileIamStore(file);
        assertThat(reopened.users().findAll()).containsExactly(user);
        assertThat(reopened.roles().findAll()).containsExactly(role);
    }

    @Test
    void findAllOnAMissingFileReturnsEmpty(@TempDir Path tempDir) {
        Path file = tempDir.resolve("does-not-exist.json");

        assertThat(new JsonFileIamStore(file).users().findAll()).isEmpty();
    }
}
