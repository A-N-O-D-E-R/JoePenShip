package io.virtualization.sdk.iam;

import java.util.List;
import java.util.Optional;

public interface RoleRepository {
    void save(Role role);
    Optional<Role> findById(RoleId id);
    Optional<Role> findByName(String name);
    List<Role> findAll();
    void delete(RoleId id);
}
