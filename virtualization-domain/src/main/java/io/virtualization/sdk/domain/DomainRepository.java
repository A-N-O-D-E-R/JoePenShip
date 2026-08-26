package io.virtualization.sdk.domain;

import java.util.List;
import java.util.Optional;

/** Persists {@link Domain} rows. {@link InMemoryDomainRepository} is Phase 1's only implementation. */
public interface DomainRepository {
    void save(Domain domain);
    Optional<Domain> findById(DomainId id);
    List<Domain> findAll();
    void delete(DomainId id);
}
