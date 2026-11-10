package io.virtualization.sdk.network;

import java.util.List;
import java.util.Optional;

public interface NetworkRepository {
    void save(Network network);
    Optional<Network> findById(NetworkId id);
    Optional<Network> findByName(String name);
    List<Network> findAll();
    void delete(NetworkId id);
}
