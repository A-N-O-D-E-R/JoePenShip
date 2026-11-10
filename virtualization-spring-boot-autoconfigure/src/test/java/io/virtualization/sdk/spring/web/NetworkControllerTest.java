package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.network.DefaultNetworkManager;
import io.virtualization.sdk.network.InMemoryNetworkRepository;
import io.virtualization.sdk.network.Network;
import io.virtualization.sdk.network.NetworkManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NetworkControllerTest {

    private NetworkController controller;

    @BeforeEach
    void setUp() {
        NetworkManager manager = new DefaultNetworkManager(new InMemoryNetworkRepository());
        controller = new NetworkController(manager);
    }

    @Test
    void createReturnsTheCreatedNetwork() {
        Network created = controller.create(new CreateNetworkRequestBody("public", "public-facing"));

        assertThat(created.name()).isEqualTo("public");
        assertThat(created.description()).isEqualTo("public-facing");
    }

    @Test
    void getReturnsTheCreatedNetwork() {
        Network created = controller.create(new CreateNetworkRequestBody("public", null));

        assertThat(controller.get(created.id().value())).isEqualTo(created);
    }

    @Test
    void getUnknownNetworkThrowsResourceNotFound() {
        assertThatThrownBy(() -> controller.get("network-does-not-exist")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listReturnsEveryCreatedNetwork() {
        Network created = controller.create(new CreateNetworkRequestBody("public", null));

        assertThat(controller.list()).containsExactly(created);
    }

    @Test
    void deleteRemovesTheNetwork() {
        Network created = controller.create(new CreateNetworkRequestBody("public", null));

        controller.delete(created.id().value());

        assertThat(controller.list()).isEmpty();
    }
}
