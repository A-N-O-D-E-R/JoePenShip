package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.project.Project;
import io.virtualization.sdk.project.ProjectId;
import io.virtualization.sdk.project.ProjectManager;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/** {@code /api/v1/projects} — over the always-registered {@link ProjectManager} bean (see {@link io.virtualization.sdk.spring.ProjectAutoConfiguration}). */
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectManager projectManager;

    public ProjectController(ProjectManager projectManager) {
        this.projectManager = projectManager;
    }

    @GetMapping
    public List<Project> list() {
        return projectManager.list();
    }

    @PostMapping
    public Project create(@RequestBody CreateProjectRequestBody request) {
        Objects.requireNonNull(request.name(), "'name' is required");
        return projectManager.create(request.name(), request.description());
    }

    @GetMapping("/{id}")
    public Project get(@PathVariable String id) {
        return projectManager.get(new ProjectId(id));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) {
        projectManager.delete(new ProjectId(id));
    }
}
