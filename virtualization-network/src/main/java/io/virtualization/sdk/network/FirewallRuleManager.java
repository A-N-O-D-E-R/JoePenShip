package io.virtualization.sdk.network;

import io.virtualization.sdk.core.exception.ResourceNotFoundException;

import java.util.List;

public interface FirewallRuleManager {

    /** @throws ResourceNotFoundException if {@code networkId} is unknown */
    FirewallRule create(NetworkId networkId, FirewallDirection direction, FirewallProtocol protocol,
                         Integer portRangeStart, Integer portRangeEnd, String cidr, FirewallAction action, int priority);

    /** @throws ResourceNotFoundException if {@code id} is unknown */
    FirewallRule get(FirewallRuleId id);

    List<FirewallRule> list(NetworkId networkId);

    /** @throws ResourceNotFoundException if {@code id} is unknown */
    void delete(FirewallRuleId id);
}
