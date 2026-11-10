package io.virtualization.sdk.iam;

import io.virtualization.sdk.core.exception.AuthenticationException;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** The only {@link ApiCredentialManager} implementation. */
public final class DefaultApiCredentialManager implements ApiCredentialManager {

    private final ApiCredentialRepository repository;
    private final UserRepository users;

    public DefaultApiCredentialManager(ApiCredentialRepository repository, UserRepository users) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.users = Objects.requireNonNull(users, "users must not be null");
    }

    @Override
    public IssuedApiCredential issue(UserId ownerId, String name, Duration ttl) {
        Objects.requireNonNull(ownerId, "ownerId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        if (users.findById(ownerId).isEmpty()) {
            throw new ResourceNotFoundException("No user with id '" + ownerId.value() + "'");
        }
        String rawToken = ApiTokens.generate();
        Instant now = Instant.now();
        ApiCredential credential = new ApiCredential(
                ApiCredentialId.generate(), ownerId, name, ApiTokens.hash(rawToken), now, ttl != null ? now.plus(ttl) : null, null);
        repository.save(credential);
        return new IssuedApiCredential(rawToken, credential);
    }

    @Override
    public List<ApiCredential> list(UserId ownerId) {
        Objects.requireNonNull(ownerId, "ownerId must not be null");
        return repository.findByOwner(ownerId);
    }

    @Override
    public void revoke(ApiCredentialId id) {
        Objects.requireNonNull(id, "id must not be null");
        if (repository.findById(id).isEmpty()) {
            throw new ResourceNotFoundException("No API credential with id '" + id.value() + "'");
        }
        repository.delete(id);
    }

    @Override
    public User authenticate(String rawToken) {
        Objects.requireNonNull(rawToken, "rawToken must not be null");
        ApiCredential credential = repository.findByTokenHash(ApiTokens.hash(rawToken)).orElseThrow(DefaultApiCredentialManager::invalidToken);
        if (credential.expiresAt() != null && credential.expiresAt().isBefore(Instant.now())) {
            throw invalidToken();
        }
        User user = users.findById(credential.ownerId()).orElseThrow(DefaultApiCredentialManager::invalidToken);
        if (!user.enabled()) {
            throw invalidToken();
        }
        recordUsage(credential);
        return user;
    }

    private void recordUsage(ApiCredential credential) {
        repository.save(new ApiCredential(
                credential.id(), credential.ownerId(), credential.name(), credential.tokenHash(), credential.createdAt(),
                credential.expiresAt(), Instant.now()));
    }

    private static AuthenticationException invalidToken() {
        return new AuthenticationException("Invalid API token");
    }
}
