package io.virtualization.sdk.vps;

import java.util.List;
import java.util.Optional;

/** Persists {@link Vps} rows. {@link InMemoryVpsRepository} is Phase 1's only implementation. */
public interface VpsRepository {

    void save(Vps vps);

    Optional<Vps> findById(VpsId id);

    List<Vps> findAll();

    /** Purges a row outright. Not called by {@link DefaultVpsManager}'s own lifecycle — {@code destroy} is soft-delete. */
    void delete(VpsId id);
}
