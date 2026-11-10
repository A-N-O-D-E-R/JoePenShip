package io.virtualization.sdk.network;

import io.virtualization.sdk.core.exception.ResourceNotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** The only {@link NetworkManager} implementation. */
public final class DefaultNetworkManager implements NetworkManager {

    private final NetworkRepository repository;

    public DefaultNetworkManager(NetworkRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public Network create(String name, String description) {
        Objects.requireNonNull(name, "name must not be null");
        if (repository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("network name '" + name + "' is already taken");
        }
        Network network = new Network(NetworkId.generate(), name, description, Instant.now());
        repository.save(network);
        return network;
    }

    @Override
    public Network get(NetworkId id) {
        Objects.requireNonNull(id, "id must not be null");
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("No network with id '" + id.value() + "'"));
    }

    @Override
    public List<Network> list() {
        return repository.findAll();
    }

    @Override
    public void delete(NetworkId id) {
        get(id);
        repository.delete(id);
    }
}
