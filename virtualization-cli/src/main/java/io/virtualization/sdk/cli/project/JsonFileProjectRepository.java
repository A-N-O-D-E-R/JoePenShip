package io.virtualization.sdk.cli.project;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.virtualization.sdk.project.Project;
import io.virtualization.sdk.project.ProjectId;
import io.virtualization.sdk.project.ProjectRepository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@link ProjectRepository} backed by a single JSON file — exact mirror of {@code
 * JsonFileVpsRepository}'s pattern; the CLI has no long-lived process to keep an {@code
 * InMemoryProjectRepository} alive between invocations.
 *
 * <p>ponytail: whole-file read-modify-write under an in-process lock only, no OS-level file
 * locking — same accepted tradeoff as every other {@code JsonFile*} repository in this codebase.
 */
public final class JsonFileProjectRepository implements ProjectRepository {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final Path file;

    public JsonFileProjectRepository(Path file) {
        this.file = file;
    }

    @Override
    public synchronized void save(Project project) {
        Map<ProjectId, Project> all = readAll();
        all.put(project.id(), project);
        writeAll(all);
    }

    @Override
    public synchronized Optional<Project> findById(ProjectId id) {
        return Optional.ofNullable(readAll().get(id));
    }

    @Override
    public synchronized Optional<Project> findByName(String name) {
        return readAll().values().stream().filter(p -> p.name().equals(name)).findFirst();
    }

    @Override
    public synchronized List<Project> findAll() {
        return List.copyOf(readAll().values());
    }

    @Override
    public synchronized void delete(ProjectId id) {
        Map<ProjectId, Project> all = readAll();
        all.remove(id);
        writeAll(all);
    }

    private Map<ProjectId, Project> readAll() {
        if (!Files.exists(file)) {
            return new LinkedHashMap<>();
        }
        try {
            Project[] records = MAPPER.readValue(file.toFile(), Project[].class);
            Map<ProjectId, Project> map = new LinkedHashMap<>();
            for (Project project : records) {
                map.put(project.id(), project);
            }
            return map;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read project state file '" + file + "'", e);
        }
    }

    private void writeAll(Map<ProjectId, Project> all) {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            MAPPER.writeValue(file.toFile(), all.values());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write project state file '" + file + "'", e);
        }
    }
}
