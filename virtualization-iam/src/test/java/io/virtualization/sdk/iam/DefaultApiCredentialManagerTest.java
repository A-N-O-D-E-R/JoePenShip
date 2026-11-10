package io.virtualization.sdk.iam;

import io.virtualization.sdk.core.exception.AuthenticationException;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultApiCredentialManagerTest {

    private UserRepository users;
    private DefaultApiCredentialManager manager;
    private User owner;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        manager = new DefaultApiCredentialManager(new InMemoryApiCredentialRepository(), users);
        owner = new DefaultUserManager(users).create("alice", "s3cret!".toCharArray());
    }

    @Test
    void issueThenAuthenticateReturnsTheOwner() {
        var issued = manager.issue(owner.id(), "ci-token", null);

        User authenticated = manager.authenticate(issued.rawToken());

        assertThat(authenticated.id()).isEqualTo(owner.id());
    }

    @Test
    void authenticateFailsForAnUnknownToken() {
        assertThatThrownBy(() -> manager.authenticate("vsdk_does-not-exist")).isInstanceOf(AuthenticationException.class);
    }

    @Test
    void authenticateFailsForAnExpiredToken() {
        var issued = manager.issue(owner.id(), "short-lived", Duration.ofSeconds(-1));

        assertThatThrownBy(() -> manager.authenticate(issued.rawToken())).isInstanceOf(AuthenticationException.class);
    }

    @Test
    void authenticateFailsAfterRevocation() {
        var issued = manager.issue(owner.id(), "revoked-token", null);

        manager.revoke(issued.credential().id());

        assertThatThrownBy(() -> manager.authenticate(issued.rawToken())).isInstanceOf(AuthenticationException.class);
    }

    @Test
    void issueRejectsAnUnknownOwner() {
        assertThatThrownBy(() -> manager.issue(UserId.generate(), "token", null)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void authenticateRecordsLastUsedAt() {
        var issued = manager.issue(owner.id(), "token", null);
        assertThat(issued.credential().lastUsedAt()).isNull();

        manager.authenticate(issued.rawToken());

        var updated = manager.list(owner.id()).stream().filter(c -> c.id().equals(issued.credential().id())).findFirst().orElseThrow();
        assertThat(updated.lastUsedAt()).isNotNull();
    }
}
