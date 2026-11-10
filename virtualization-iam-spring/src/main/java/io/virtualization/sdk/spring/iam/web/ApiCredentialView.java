package io.virtualization.sdk.spring.iam.web;

import io.virtualization.sdk.iam.ApiCredential;

import java.time.Instant;

/**
 * {@link ApiCredential} minus {@code tokenHash} — never serialize a token hash over REST, and the
 * raw token itself is only ever returned once, from {@link IssuedApiCredentialView}.
 */
public record ApiCredentialView(String id, String ownerId, String name, Instant createdAt, Instant expiresAt, Instant lastUsedAt) {

    public static ApiCredentialView from(ApiCredential credential) {
        return new ApiCredentialView(
                credential.id().value(), credential.ownerId().value(), credential.name(), credential.createdAt(),
                credential.expiresAt(), credential.lastUsedAt());
    }
}
