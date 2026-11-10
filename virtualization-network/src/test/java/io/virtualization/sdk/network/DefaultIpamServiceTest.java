package io.virtualization.sdk.network;

import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultIpamServiceTest {

    private InMemoryNetworkRepository networkRepository;
    private InMemorySubnetRepository subnetRepository;
    private DefaultIpamService ipamService;
    private DefaultNetworkManager networkManager;
    private DefaultSubnetManager subnetManager;

    @BeforeEach
    void setUp() {
        networkRepository = new InMemoryNetworkRepository();
        subnetRepository = new InMemorySubnetRepository();
        networkManager = new DefaultNetworkManager(networkRepository);
        subnetManager = new DefaultSubnetManager(subnetRepository, networkRepository);
        ipamService = new DefaultIpamService(subnetRepository, new InMemoryIpAllocationRepository());
    }

    @Test
    void allocateReturnsTheFirstUsableAddress() {
        Subnet subnet = subnetManager.create(networkManager.create("public", null).id(), "10.0.0.0/30", null);

        IpAllocation allocation = ipamService.allocate(subnet.id(), "vps-1");

        assertThat(allocation.address()).isEqualTo("10.0.0.1");
        assertThat(allocation.ownerRef()).isEqualTo("vps-1");
    }

    @Test
    void allocateSkipsTheGatewayAndAlreadyAllocatedAddresses() {
        Subnet subnet = subnetManager.create(networkManager.create("public", null).id(), "10.0.0.0/29", "10.0.0.1");

        IpAllocation first = ipamService.allocate(subnet.id(), "vps-1");
        IpAllocation second = ipamService.allocate(subnet.id(), "vps-2");

        assertThat(first.address()).isEqualTo("10.0.0.2");
        assertThat(second.address()).isEqualTo("10.0.0.3");
    }

    @Test
    void releaseFreesTheAddressForReallocation() {
        Subnet subnet = subnetManager.create(networkManager.create("public", null).id(), "10.0.0.0/30", null);
        IpAllocation allocation = ipamService.allocate(subnet.id(), "vps-1");

        ipamService.release(allocation.id());
        IpAllocation reallocated = ipamService.allocate(subnet.id(), "vps-2");

        assertThat(reallocated.address()).isEqualTo(allocation.address());
    }

    @Test
    void releaseIsIdempotent() {
        assertThatCode(() -> ipamService.release(IpAllocationId.generate())).doesNotThrowAnyException();
    }

    @Test
    void allocateThrowsWhenTheSubnetIsExhausted() {
        Subnet subnet = subnetManager.create(networkManager.create("public", null).id(), "10.0.0.0/30", null);
        ipamService.allocate(subnet.id(), "vps-1");
        ipamService.allocate(subnet.id(), "vps-2");

        assertThatThrownBy(() -> ipamService.allocate(subnet.id(), "vps-3")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void allocateThrowsForAnUnknownSubnet() {
        assertThatThrownBy(() -> ipamService.allocate(SubnetId.generate(), "vps-1")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void allocateThrowsForAnIpv6Subnet() {
        Subnet subnet = subnetManager.create(networkManager.create("public", null).id(), "fd00::/64", null);

        assertThatThrownBy(() -> ipamService.allocate(subnet.id(), "vps-1")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void listReturnsAllocationsForTheGivenSubnetOnly() {
        Subnet subnet = subnetManager.create(networkManager.create("public", null).id(), "10.0.0.0/29", null);
        Subnet other = subnetManager.create(networkManager.create("private", null).id(), "10.0.1.0/29", null);
        IpAllocation allocation = ipamService.allocate(subnet.id(), "vps-1");
        ipamService.allocate(other.id(), "vps-2");

        assertThat(ipamService.list(subnet.id())).containsExactly(allocation);
    }
}
