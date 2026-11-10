package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.network.DefaultIpamService;
import io.virtualization.sdk.network.DefaultNetworkManager;
import io.virtualization.sdk.network.DefaultSubnetManager;
import io.virtualization.sdk.network.InMemoryIpAllocationRepository;
import io.virtualization.sdk.network.InMemoryNetworkRepository;
import io.virtualization.sdk.network.InMemorySubnetRepository;
import io.virtualization.sdk.network.IpAllocation;
import io.virtualization.sdk.network.IpamService;
import io.virtualization.sdk.network.Network;
import io.virtualization.sdk.network.NetworkManager;
import io.virtualization.sdk.network.Subnet;
import io.virtualization.sdk.network.SubnetManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IpAllocationControllerTest {

    private IpAllocationController controller;
    private Network network;
    private Subnet subnet;

    @BeforeEach
    void setUp() {
        InMemoryNetworkRepository networkRepository = new InMemoryNetworkRepository();
        InMemorySubnetRepository subnetRepository = new InMemorySubnetRepository();
        NetworkManager networkManager = new DefaultNetworkManager(networkRepository);
        SubnetManager subnetManager = new DefaultSubnetManager(subnetRepository, networkRepository);
        IpamService ipamService = new DefaultIpamService(subnetRepository, new InMemoryIpAllocationRepository());
        controller = new IpAllocationController(ipamService);
        network = networkManager.create("public", null);
        subnet = subnetManager.create(network.id(), "10.0.0.0/29", null);
    }

    @Test
    void allocateReturnsTheFirstUsableAddress() {
        IpAllocation allocation = controller.allocate(network.id().value(), subnet.id().value(), new AllocateIpRequestBody("vps-1"));

        assertThat(allocation.address()).isEqualTo("10.0.0.1");
        assertThat(allocation.ownerRef()).isEqualTo("vps-1");
    }

    @Test
    void listReturnsEveryAllocationInTheSubnet() {
        IpAllocation allocation = controller.allocate(network.id().value(), subnet.id().value(), new AllocateIpRequestBody("vps-1"));

        assertThat(controller.list(network.id().value(), subnet.id().value())).containsExactly(allocation);
    }

    @Test
    void releaseFreesTheAllocation() {
        IpAllocation allocation = controller.allocate(network.id().value(), subnet.id().value(), new AllocateIpRequestBody("vps-1"));

        controller.release(network.id().value(), subnet.id().value(), allocation.id().value());

        assertThat(controller.list(network.id().value(), subnet.id().value())).isEmpty();
    }
}
