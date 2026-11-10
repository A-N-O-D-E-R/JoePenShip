package io.virtualization.sdk.iam;

import java.time.Instant;
import java.util.Objects;

/**
 * A machine credential — {@code tokenHash} is {@link ApiTokens#hash} of the raw token, never the
 * token itself; the raw value exists only transiently, returned once from {@code
 * ApiCredentialManager#issue}. {@link #toString()} redacts it.
 *
 * @param expiresAt  {@code null} = never expires
 * @param lastUsedAt {@code null} until the first successful {@code ApiCredentialManager#authenticate}
 */
public record ApiCredential(ApiCredentialId id, UserId ownerId, String name, String tokenHash, Instant createdAt, Instant expiresAt, Instant lastUsedAt) {

    public ApiCredential {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(ownerId, "ownerId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(tokenHash, "tokenHash must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }

    @Override
    public String toString() {
        return "ApiCredential[id=" + id + ", ownerId=" + ownerId + ", name=" + name + ", tokenHash=<redacted>, createdAt=" + createdAt
                + ", expiresAt=" + expiresAt + ", lastUsedAt=" + lastUsedAt + "]";
    }
}
