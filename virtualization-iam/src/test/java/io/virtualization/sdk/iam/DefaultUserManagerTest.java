package io.virtualization.sdk.iam;

import io.virtualization.sdk.core.exception.AuthenticationException;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultUserManagerTest {

    private DefaultUserManager manager;

    @BeforeEach
    void setUp() {
        manager = new DefaultUserManager(new InMemoryUserRepository());
    }

    @Test
    void createRejectsADuplicateUsername() {
        manager.create("alice", "password1".toCharArray());

        assertThatThrownBy(() -> manager.create("alice", "password2".toCharArray())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void authenticateSucceedsWithTheCorrectPassword() {
        User created = manager.create("alice", "s3cret!".toCharArray());

        User authenticated = manager.authenticate("alice", "s3cret!".toCharArray());

        assertThat(authenticated.id()).isEqualTo(created.id());
    }

    @Test
    void authenticateFailsWithTheWrongPassword() {
        manager.create("alice", "s3cret!".toCharArray());

        assertThatThrownBy(() -> manager.authenticate("alice", "wrong".toCharArray())).isInstanceOf(AuthenticationException.class);
    }

    @Test
    void authenticateFailsForAnUnknownUsername() {
        assertThatThrownBy(() -> manager.authenticate("nobody", "x".toCharArray())).isInstanceOf(AuthenticationException.class);
    }

    @Test
    void authenticateFailsForADisabledUser() {
        User created = manager.create("alice", "s3cret!".toCharArray());
        manager.setEnabled(created.id(), false);

        assertThatThrownBy(() -> manager.authenticate("alice", "s3cret!".toCharArray())).isInstanceOf(AuthenticationException.class);
    }

    @Test
    void assignRoleThenRemoveRoleRoundTrips() {
        User created = manager.create("alice", "s3cret!".toCharArray());
        RoleId roleId = RoleId.generate();

        User withRole = manager.assignRole(created.id(), roleId);
        assertThat(withRole.roles()).containsExactly(roleId);

        User withoutRole = manager.removeRole(created.id(), roleId);
        assertThat(withoutRole.roles()).isEmpty();
    }

    @Test
    void getUnknownUserThrowsResourceNotFound() {
        assertThatThrownBy(() -> manager.get(UserId.generate())).isInstanceOf(ResourceNotFoundException.class);
    }
}
