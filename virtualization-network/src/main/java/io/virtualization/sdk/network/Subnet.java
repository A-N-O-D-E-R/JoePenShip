package io.virtualization.sdk.network;

import java.time.Instant;
import java.util.Objects;

/**
 * A CIDR block within a {@link Network} (e.g. {@code "10.0.0.0/24"}). {@code gateway} is an
 * optional reserved address, excluded from {@link IpamService} allocation alongside the network
 * and broadcast addresses. IPv6 CIDRs are accepted and stored but {@link IpamService#allocate}
 * doesn't support them — see {@link CidrBlock}.
 */
public record Subnet(SubnetId id, NetworkId networkId, String cidr, String gateway, Instant createdAt) {

    public Subnet {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(networkId, "networkId must not be null");
        Objects.requireNonNull(cidr, "cidr must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        CidrBlock block = CidrBlock.parse(cidr);
        if (gateway != null && block.isIpv4() && !block.contains(gateway)) {
            throw new IllegalArgumentException("gateway '" + gateway + "' is not within CIDR '" + cidr + "'");
        }
    }

    CidrBlock cidrBlock() {
        return CidrBlock.parse(cidr);
    }
}
