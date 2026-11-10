package io.virtualization.sdk.network;

import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultSubnetManagerTest {

    private DefaultNetworkManager networkManager;
    private DefaultSubnetManager subnetManager;

    @BeforeEach
    void setUp() {
        InMemoryNetworkRepository networkRepository = new InMemoryNetworkRepository();
        networkManager = new DefaultNetworkManager(networkRepository);
        subnetManager = new DefaultSubnetManager(new InMemorySubnetRepository(), networkRepository);
    }

    @Test
    void createThenGetRoundTrips() {
        Network network = networkManager.create("public", null);

        Subnet created = subnetManager.create(network.id(), "10.0.0.0/24", "10.0.0.1");

        assertThat(subnetManager.get(created.id())).isEqualTo(created);
    }

    @Test
    void createRejectsAnUnknownNetwork() {
        assertThatThrownBy(() -> subnetManager.create(NetworkId.generate(), "10.0.0.0/24", null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createRejectsAnOverlappingCidrInTheSameNetwork() {
        Network network = networkManager.create("public", null);
        subnetManager.create(network.id(), "10.0.0.0/24", null);

        assertThatThrownBy(() -> subnetManager.create(network.id(), "10.0.0.128/25", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createAllowsTheSameCidrInADifferentNetwork() {
        Network first = networkManager.create("public", null);
        Network second = networkManager.create("private", null);
        subnetManager.create(first.id(), "10.0.0.0/24", null);

        Subnet inSecond = subnetManager.create(second.id(), "10.0.0.0/24", null);

        assertThat(inSecond.cidr()).isEqualTo("10.0.0.0/24");
    }

    @Test
    void listReturnsOnlySubnetsInTheGivenNetwork() {
        Network first = networkManager.create("public", null);
        Network second = networkManager.create("private", null);
        Subnet inFirst = subnetManager.create(first.id(), "10.0.0.0/24", null);
        subnetManager.create(second.id(), "10.0.1.0/24", null);

        assertThat(subnetManager.list(first.id())).containsExactly(inFirst);
    }

    @Test
    void deleteRemovesTheSubnet() {
        Network network = networkManager.create("public", null);
        Subnet created = subnetManager.create(network.id(), "10.0.0.0/24", null);

        subnetManager.delete(created.id());

        assertThatThrownBy(() -> subnetManager.get(created.id())).isInstanceOf(ResourceNotFoundException.class);
    }
}
