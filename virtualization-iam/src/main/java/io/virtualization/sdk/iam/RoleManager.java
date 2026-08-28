package io.virtualization.sdk.iam;

import io.virtualization.sdk.core.exception.ResourceNotFoundException;

import java.util.List;

public interface RoleManager {

    /** @throws IllegalArgumentException if {@code name} is already taken */
    Role create(String name);

    /** @throws ResourceNotFoundException if {@code id} is unknown */
    Role get(RoleId id);

    List<Role> list();

    /** @throws ResourceNotFoundException if {@code id} is unknown */
    void delete(RoleId id);

    /** @throws ResourceNotFoundException if {@code id} is unknown */
    Role grantPermission(RoleId id, Permission permission);

    /** @throws ResourceNotFoundException if {@code id} is unknown */
    Role revokePermission(RoleId id, Permission permission);
}
