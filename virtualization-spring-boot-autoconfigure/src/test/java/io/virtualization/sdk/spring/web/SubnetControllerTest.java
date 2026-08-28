package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.network.DefaultNetworkManager;
import io.virtualization.sdk.network.DefaultSubnetManager;
import io.virtualization.sdk.network.InMemoryNetworkRepository;
import io.virtualization.sdk.network.InMemorySubnetRepository;
import io.virtualization.sdk.network.Network;
import io.virtualization.sdk.network.NetworkManager;
import io.virtualization.sdk.network.Subnet;
import io.virtualization.sdk.network.SubnetManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubnetControllerTest {

    private SubnetController controller;
    private Network network;

    @BeforeEach
    void setUp() {
        InMemoryNetworkRepository networkRepository = new InMemoryNetworkRepository();
        NetworkManager networkManager = new DefaultNetworkManager(networkRepository);
        SubnetManager subnetManager = new DefaultSubnetManager(new InMemorySubnetRepository(), networkRepository);
        controller = new SubnetController(subnetManager);
        network = networkManager.create("public", null);
    }

    @Test
    void createReturnsTheCreatedSubnet() {
        Subnet created = controller.create(network.id().value(), new CreateSubnetRequestBody("10.0.0.0/24", "10.0.0.1"));

        assertThat(created.cidr()).isEqualTo("10.0.0.0/24");
        assertThat(created.gateway()).isEqualTo("10.0.0.1");
    }

    @Test
    void getReturnsTheCreatedSubnet() {
        Subnet created = controller.create(network.id().value(), new CreateSubnetRequestBody("10.0.0.0/24", null));

        assertThat(controller.get(network.id().value(), created.id().value())).isEqualTo(created);
    }

    @Test
    void getUnknownSubnetThrowsResourceNotFound() {
        assertThatThrownBy(() -> controller.get(network.id().value(), "subnet-does-not-exist")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listReturnsEveryCreatedSubnetInTheNetwork() {
        Subnet created = controller.create(network.id().value(), new CreateSubnetRequestBody("10.0.0.0/24", null));

        assertThat(controller.list(network.id().value())).containsExactly(created);
    }

    @Test
    void deleteRemovesTheSubnet() {
        Subnet created = controller.create(network.id().value(), new CreateSubnetRequestBody("10.0.0.0/24", null));

        controller.delete(network.id().value(), created.id().value());

        assertThat(controller.list(network.id().value())).isEmpty();
    }
}
