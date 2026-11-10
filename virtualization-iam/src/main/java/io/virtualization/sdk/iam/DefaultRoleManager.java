package io.virtualization.sdk.iam;

import io.virtualization.sdk.core.exception.ResourceNotFoundException;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** The only {@link RoleManager} implementation. */
public final class DefaultRoleManager implements RoleManager {

    private final RoleRepository repository;

    public DefaultRoleManager(RoleRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public Role create(String name) {
        Objects.requireNonNull(name, "name must not be null");
        if (repository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("role name '" + name + "' is already taken");
        }
        Role role = new Role(RoleId.generate(), name, Set.of(), Instant.now());
        repository.save(role);
        return role;
    }

    @Override
    public Role get(RoleId id) {
        Objects.requireNonNull(id, "id must not be null");
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("No role with id '" + id.value() + "'"));
    }

    @Override
    public List<Role> list() {
        return repository.findAll();
    }

    @Override
    public void delete(RoleId id) {
        get(id);
        repository.delete(id);
    }

    @Override
    public Role grantPermission(RoleId id, Permission permission) {
        Role current = get(id);
        Set<Permission> permissions = new HashSet<>(current.permissions());
        permissions.add(permission);
        Role updated = new Role(current.id(), current.name(), permissions, current.createdAt());
        repository.save(updated);
        return updated;
    }

    @Override
    public Role revokePermission(RoleId id, Permission permission) {
        Role current = get(id);
        Set<Permission> permissions = new HashSet<>(current.permissions());
        permissions.remove(permission);
        Role updated = new Role(current.id(), current.name(), permissions, current.createdAt());
        repository.save(updated);
        return updated;
    }
}
