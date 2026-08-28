package io.virtualization.sdk.project;

import io.virtualization.sdk.core.exception.ResourceNotFoundException;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** The only {@link ProjectManager} implementation. */
public final class DefaultProjectManager implements ProjectManager {

    private final ProjectRepository repository;

    public DefaultProjectManager(ProjectRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    @Override
    public Project create(String name, String description) {
        Objects.requireNonNull(name, "name must not be null");
        if (repository.findByName(name).isPresent()) {
            throw new IllegalArgumentException("project name '" + name + "' is already taken");
        }
        Project project = new Project(ProjectId.generate(), name, description, Instant.now());
        repository.save(project);
        return project;
    }

    @Override
    public Project get(ProjectId id) {
        Objects.requireNonNull(id, "id must not be null");
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("No project with id '" + id.value() + "'"));
    }

    @Override
    public List<Project> list() {
        return repository.findAll();
    }

    @Override
    public void delete(ProjectId id) {
        get(id);
        repository.delete(id);
    }
}
