package io.virtualization.sdk.network;

import java.time.Instant;
import java.util.Objects;

/**
 * A firewall rule scoped to a {@link Network}. Modeled only — no provider (Proxmox/Incus) in this
 * SDK enforces it yet, the same "mapped in the domain model, no interface method yet" status this
 * SDK already accepts for Proxmox LXC containers and Incus networks/storage/snapshots.
 */
public record FirewallRule(
        FirewallRuleId id,
        NetworkId networkId,
        FirewallDirection direction,
        FirewallProtocol protocol,
        Integer portRangeStart,
        Integer portRangeEnd,
        String cidr,
        FirewallAction action,
        int priority,
        Instant createdAt) {

    public FirewallRule {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(networkId, "networkId must not be null");
        Objects.requireNonNull(direction, "direction must not be null");
        Objects.requireNonNull(protocol, "protocol must not be null");
        Objects.requireNonNull(cidr, "cidr must not be null");
        Objects.requireNonNull(action, "action must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if ((portRangeStart == null) != (portRangeEnd == null)) {
            throw new IllegalArgumentException("portRangeStart and portRangeEnd must both be set or both be null");
        }
        if (portRangeStart != null && portRangeStart > portRangeEnd) {
            throw new IllegalArgumentException("portRangeStart must not be greater than portRangeEnd");
        }
    }
}
