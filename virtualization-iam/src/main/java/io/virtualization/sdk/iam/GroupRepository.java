package io.virtualization.sdk.iam;

import java.util.List;
import java.util.Optional;

public interface GroupRepository {
    void save(Group group);
    Optional<Group> findById(GroupId id);
    Optional<Group> findByName(String name);
    List<Group> findAll();
    void delete(GroupId id);
}
