package io.virtualization.sdk.network.store;

import io.virtualization.sdk.network.FirewallRule;
import io.virtualization.sdk.network.IpAllocation;
import io.virtualization.sdk.network.Network;
import io.virtualization.sdk.network.Subnet;

import java.util.List;

/** The on-disk shape of {@link JsonFileNetworkStore}'s single JSON file. */
record NetworkFile(List<Network> networks, List<Subnet> subnets, List<IpAllocation> ipAllocations, List<FirewallRule> firewallRules) {

    static final NetworkFile EMPTY = new NetworkFile(List.of(), List.of(), List.of(), List.of());
}
