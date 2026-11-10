package io.virtualization.sdk.project;

import io.virtualization.sdk.core.exception.ResourceNotFoundException;

import java.util.List;

public interface ProjectManager {

    /** @throws IllegalArgumentException if {@code name} is already taken */
    Project create(String name, String description);

    /** @throws ResourceNotFoundException if {@code id} is unknown */
    Project get(ProjectId id);

    List<Project> list();

    /** @throws ResourceNotFoundException if {@code id} is unknown */
    void delete(ProjectId id);
}
