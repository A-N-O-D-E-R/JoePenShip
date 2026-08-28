package io.virtualization.sdk.iam;

import io.virtualization.sdk.core.exception.AuthenticationException;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** The only {@link UserManager} implementation. */
public final class DefaultUserManager implements UserManager {

    private final UserRepository repository;

    public DefaultUserManager(UserRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public User create(String username, char[] password) {
        Objects.requireNonNull(username, "username must not be null");
        if (repository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("username '" + username + "' is already taken");
        }
        User user = new User(UserId.generate(), username, PasswordHasher.hash(password), Set.of(), Set.of(), true, Instant.now());
        repository.save(user);
        return user;
    }

    @Override
    public User get(UserId id) {
        Objects.requireNonNull(id, "id must not be null");
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("No user with id '" + id.value() + "'"));
    }

    @Override
    public List<User> list() {
        return repository.findAll();
    }

    @Override
    public User setEnabled(UserId id, boolean enabled) {
        User current = get(id);
        User updated = new User(
                current.id(), current.username(), current.passwordHash(), current.roles(), current.groups(), enabled, current.createdAt());
        repository.save(updated);
        return updated;
    }

    @Override
    public User setPassword(UserId id, char[] newPassword) {
        User current = get(id);
        User updated = new User(
                current.id(), current.username(), PasswordHasher.hash(newPassword), current.roles(), current.groups(), current.enabled(),
                current.createdAt());
        repository.save(updated);
        return updated;
    }

    @Override
    public User assignRole(UserId id, RoleId roleId) {
        User current = get(id);
        Set<RoleId> roles = new HashSet<>(current.roles());
        roles.add(roleId);
        User updated = new User(current.id(), current.username(), current.passwordHash(), roles, current.groups(), current.enabled(), current.createdAt());
        repository.save(updated);
        return updated;
    }

    @Override
    public User removeRole(UserId id, RoleId roleId) {
        User current = get(id);
        Set<RoleId> roles = new HashSet<>(current.roles());
        roles.remove(roleId);
        User updated = new User(current.id(), current.username(), current.passwordHash(), roles, current.groups(), current.enabled(), current.createdAt());
        repository.save(updated);
        return updated;
    }

    @Override
    public User authenticate(String username, char[] password) {
        Objects.requireNonNull(username, "username must not be null");
        // Same "invalid username or password" message whether the username is unknown or the
        // password is wrong — distinguishing the two lets a caller enumerate valid usernames.
        User user = repository.findByUsername(username).orElseThrow(DefaultUserManager::invalidCredentials);
        if (!user.enabled() || !PasswordHasher.verify(password, user.passwordHash())) {
            throw invalidCredentials();
        }
        return user;
    }

    private static AuthenticationException invalidCredentials() {
        return new AuthenticationException("Invalid username or password");
    }
}
