package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.project.DefaultProjectManager;
import io.virtualization.sdk.project.InMemoryProjectRepository;
import io.virtualization.sdk.project.Project;
import io.virtualization.sdk.project.ProjectManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectControllerTest {

    private ProjectController controller;

    @BeforeEach
    void setUp() {
        ProjectManager manager = new DefaultProjectManager(new InMemoryProjectRepository());
        controller = new ProjectController(manager);
    }

    @Test
    void createReturnsTheCreatedProject() {
        Project created = controller.create(new CreateProjectRequestBody("homelab", "personal infra"));

        assertThat(created.name()).isEqualTo("homelab");
        assertThat(created.description()).isEqualTo("personal infra");
    }

    @Test
    void getReturnsTheCreatedProject() {
        Project created = controller.create(new CreateProjectRequestBody("homelab", null));

        assertThat(controller.get(created.id().value())).isEqualTo(created);
    }

    @Test
    void getUnknownProjectThrowsResourceNotFound() {
        assertThatThrownBy(() -> controller.get("project-does-not-exist")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listReturnsEveryCreatedProject() {
        Project created = controller.create(new CreateProjectRequestBody("homelab", null));

        assertThat(controller.list()).containsExactly(created);
    }

    @Test
    void deleteRemovesTheProject() {
        Project created = controller.create(new CreateProjectRequestBody("homelab", null));

        controller.delete(created.id().value());

        assertThat(controller.list()).isEmpty();
    }
}
