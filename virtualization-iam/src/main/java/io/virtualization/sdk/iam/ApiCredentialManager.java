package io.virtualization.sdk.iam;

import io.virtualization.sdk.core.exception.AuthenticationException;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;

import java.time.Duration;
import java.util.List;

public interface ApiCredentialManager {

    /**
     * @throws ResourceNotFoundException if {@code ownerId} is unknown
     * @return the raw token (shown once) alongside the persisted {@link ApiCredential} metadata
     */
    IssuedApiCredential issue(UserId ownerId, String name, Duration ttl);

    List<ApiCredential> list(UserId ownerId);

    /** @throws ResourceNotFoundException if {@code id} is unknown */
    void revoke(ApiCredentialId id);

    /** @throws AuthenticationException if {@code rawToken} is unknown, expired, or its owner is disabled */
    User authenticate(String rawToken);

    record IssuedApiCredential(String rawToken, ApiCredential credential) {}
}
