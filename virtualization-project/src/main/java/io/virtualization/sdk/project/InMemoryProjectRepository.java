package io.virtualization.sdk.project;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryProjectRepository implements ProjectRepository {

    private final ConcurrentHashMap<ProjectId, Project> store = new ConcurrentHashMap<>();

    @Override
    public void save(Project project) {
        store.put(project.id(), project);
    }

    @Override
    public Optional<Project> findById(ProjectId id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Project> findByName(String name) {
        return store.values().stream().filter(p -> p.name().equals(name)).findFirst();
    }

    @Override
    public List<Project> findAll() {
        return List.copyOf(store.values());
    }

    @Override
    public void delete(ProjectId id) {
        store.remove(id);
    }
}
