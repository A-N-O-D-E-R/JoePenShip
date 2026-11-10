package io.virtualization.sdk.network;

import io.virtualization.sdk.core.exception.ResourceNotFoundException;

import java.util.List;

public interface NetworkManager {

    /** @throws IllegalArgumentException if {@code name} is already taken */
    Network create(String name, String description);

    /** @throws ResourceNotFoundException if {@code id} is unknown */
    Network get(NetworkId id);

    List<Network> list();

    /** @throws ResourceNotFoundException if {@code id} is unknown */
    void delete(NetworkId id);
}
