package io.virtualization.sdk.iam;

import java.util.List;
import java.util.Optional;

public interface ApiCredentialRepository {
    void save(ApiCredential credential);
    Optional<ApiCredential> findById(ApiCredentialId id);
    Optional<ApiCredential> findByTokenHash(String tokenHash);
    List<ApiCredential> findByOwner(UserId ownerId);
    List<ApiCredential> findAll();
    void delete(ApiCredentialId id);
}
