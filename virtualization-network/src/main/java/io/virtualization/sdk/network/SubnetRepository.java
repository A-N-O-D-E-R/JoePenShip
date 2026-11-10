package io.virtualization.sdk.network;

import java.util.List;
import java.util.Optional;

public interface SubnetRepository {
    void save(Subnet subnet);
    Optional<Subnet> findById(SubnetId id);
    List<Subnet> findByNetwork(NetworkId networkId);
    List<Subnet> findAll();
    void delete(SubnetId id);
}
