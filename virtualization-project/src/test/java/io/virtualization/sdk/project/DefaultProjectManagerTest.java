package io.virtualization.sdk.project;

import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultProjectManagerTest {

    private DefaultProjectManager manager;

    @BeforeEach
    void setUp() {
        manager = new DefaultProjectManager(new InMemoryProjectRepository());
    }

    @Test
    void createThenGetRoundTrips() {
        Project created = manager.create("homelab", "personal infra");

        Project fetched = manager.get(created.id());

        assertThat(fetched).isEqualTo(created);
        assertThat(fetched.name()).isEqualTo("homelab");
        assertThat(fetched.description()).isEqualTo("personal infra");
    }

    @Test
    void createAllowsANullDescription() {
        Project created = manager.create("homelab", null);

        assertThat(created.description()).isNull();
    }

    @Test
    void createRejectsADuplicateName() {
        manager.create("homelab", null);

        assertThatThrownBy(() -> manager.create("homelab", "different description")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void listReturnsEveryCreatedProject() {
        Project first = manager.create("homelab", null);
        Project second = manager.create("staging", null);

        assertThat(manager.list()).containsExactlyInAnyOrder(first, second);
    }

    @Test
    void deleteRemovesTheProject() {
        Project created = manager.create("homelab", null);

        manager.delete(created.id());

        assertThatThrownBy(() -> manager.get(created.id())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getUnknownProjectThrowsResourceNotFound() {
        assertThatThrownBy(() -> manager.get(ProjectId.generate())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteUnknownProjectThrowsResourceNotFound() {
        assertThatThrownBy(() -> manager.delete(ProjectId.generate())).isInstanceOf(ResourceNotFoundException.class);
    }
}
