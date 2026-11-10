package io.virtualization.sdk.project;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository {
    void save(Project project);
    Optional<Project> findById(ProjectId id);
    Optional<Project> findByName(String name);
    List<Project> findAll();
    void delete(ProjectId id);
}
