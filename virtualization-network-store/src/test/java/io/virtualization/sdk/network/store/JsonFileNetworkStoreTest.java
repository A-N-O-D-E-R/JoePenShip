package io.virtualization.sdk.network.store;

import io.virtualization.sdk.network.FirewallAction;
import io.virtualization.sdk.network.FirewallDirection;
import io.virtualization.sdk.network.FirewallProtocol;
import io.virtualization.sdk.network.FirewallRule;
import io.virtualization.sdk.network.FirewallRuleId;
import io.virtualization.sdk.network.IpAllocation;
import io.virtualization.sdk.network.IpAllocationId;
import io.virtualization.sdk.network.Network;
import io.virtualization.sdk.network.NetworkId;
import io.virtualization.sdk.network.Subnet;
import io.virtualization.sdk.network.SubnetId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves real file persistence, not just in-JVM object reuse: writes via one {@link
 * JsonFileNetworkStore} instance, reads via a *second*, freshly-constructed instance pointed at
 * the same file — mirrors {@code JsonFileIamStore}'s own test rationale.
 */
class JsonFileNetworkStoreTest {

    @Test
    void aNetworkSavedByOneInstanceIsVisibleToAFreshInstanceOnTheSameFile(@TempDir Path tempDir) {
        Path file = tempDir.resolve("network.json");
        Network network = new Network(NetworkId.generate(), "public", "public-facing", Instant.now());

        new JsonFileNetworkStore(file).networks().save(network);

        Network reloaded = new JsonFileNetworkStore(file).networks().findById(network.id()).orElseThrow();
        assertThat(reloaded.name()).isEqualTo("public");
    }

    @Test
    void aSubnetSavedByOneInstanceIsVisibleToAFreshInstanceOnTheSameFile(@TempDir Path tempDir) {
        Path file = tempDir.resolve("network.json");
        NetworkId networkId = NetworkId.generate();
        Subnet subnet = new Subnet(SubnetId.generate(), networkId, "10.0.0.0/24", "10.0.0.1", Instant.now());

        new JsonFileNetworkStore(file).subnets().save(subnet);

        assertThat(new JsonFileNetworkStore(file).subnets().findByNetwork(networkId)).containsExactly(subnet);
    }

    @Test
    void anIpAllocationSavedByOneInstanceIsVisibleToAFreshInstanceOnTheSameFile(@TempDir Path tempDir) {
        Path file = tempDir.resolve("network.json");
        SubnetId subnetId = SubnetId.generate();
        IpAllocation allocation = new IpAllocation(IpAllocationId.generate(), subnetId, "10.0.0.5", "vps-1", Instant.now());

        new JsonFileNetworkStore(file).ipAllocations().save(allocation);

        assertThat(new JsonFileNetworkStore(file).ipAllocations().findBySubnet(subnetId)).containsExactly(allocation);
    }

    @Test
    void aFirewallRuleSavedByOneInstanceIsVisibleToAFreshInstanceOnTheSameFile(@TempDir Path tempDir) {
        Path file = tempDir.resolve("network.json");
        NetworkId networkId = NetworkId.generate();
        FirewallRule rule = new FirewallRule(FirewallRuleId.generate(), networkId, FirewallDirection.INBOUND,
                FirewallProtocol.TCP, 80, 443, "0.0.0.0/0", FirewallAction.ALLOW, 100, Instant.now());

        new JsonFileNetworkStore(file).firewallRules().save(rule);

        assertThat(new JsonFileNetworkStore(file).firewallRules().findByNetwork(networkId)).containsExactly(rule);
    }

    @Test
    void savingDifferentAggregatesAcrossInstancesDoesNotClobberEachOther(@TempDir Path tempDir) {
        Path file = tempDir.resolve("network.json");
        Network network = new Network(NetworkId.generate(), "public", null, Instant.now());
        Subnet subnet = new Subnet(SubnetId.generate(), network.id(), "10.0.0.0/24", null, Instant.now());

        new JsonFileNetworkStore(file).networks().save(network);
        new JsonFileNetworkStore(file).subnets().save(subnet);

        JsonFileNetworkStore reopened = new JsonFileNetworkStore(file);
        assertThat(reopened.networks().findAll()).containsExactly(network);
        assertThat(reopened.subnets().findAll()).containsExactly(subnet);
    }

    @Test
    void findAllOnAMissingFileReturnsEmpty(@TempDir Path tempDir) {
        Path file = tempDir.resolve("does-not-exist.json");

        assertThat(new JsonFileNetworkStore(file).networks().findAll()).isEmpty();
    }
}
