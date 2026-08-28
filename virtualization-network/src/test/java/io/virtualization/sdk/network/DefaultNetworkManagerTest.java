package io.virtualization.sdk.network;

import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultNetworkManagerTest {

    private DefaultNetworkManager manager;

    @BeforeEach
    void setUp() {
        manager = new DefaultNetworkManager(new InMemoryNetworkRepository());
    }

    @Test
    void createThenGetRoundTrips() {
        Network created = manager.create("public", "public-facing network");

        assertThat(manager.get(created.id())).isEqualTo(created);
    }

    @Test
    void createRejectsADuplicateName() {
        manager.create("public", null);

        assertThatThrownBy(() -> manager.create("public", null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void listReturnsEveryCreatedNetwork() {
        Network first = manager.create("public", null);
        Network second = manager.create("private", null);

        assertThat(manager.list()).containsExactlyInAnyOrder(first, second);
    }

    @Test
    void deleteRemovesTheNetwork() {
        Network created = manager.create("public", null);

        manager.delete(created.id());

        assertThatThrownBy(() -> manager.get(created.id())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getUnknownNetworkThrowsResourceNotFound() {
        assertThatThrownBy(() -> manager.get(NetworkId.generate())).isInstanceOf(ResourceNotFoundException.class);
    }
}
