package io.virtualization.sdk.iam;

import io.virtualization.sdk.core.exception.ResourceNotFoundException;

import java.util.List;

public interface GroupManager {

    /** @throws IllegalArgumentException if {@code name} is already taken */
    Group create(String name);

    /** @throws ResourceNotFoundException if {@code id} is unknown */
    Group get(GroupId id);

    List<Group> list();

    /** @throws ResourceNotFoundException if {@code id} is unknown */
    void delete(GroupId id);

    /** @throws ResourceNotFoundException if {@code id} is unknown */
    Group addMember(GroupId id, UserId userId);

    /** @throws ResourceNotFoundException if {@code id} is unknown */
    Group removeMember(GroupId id, UserId userId);

    /** @throws ResourceNotFoundException if {@code id} is unknown */
    Group assignRole(GroupId id, RoleId roleId);

    /** @throws ResourceNotFoundException if {@code id} is unknown */
    Group removeRole(GroupId id, RoleId roleId);
}
