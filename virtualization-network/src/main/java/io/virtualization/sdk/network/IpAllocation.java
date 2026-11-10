package io.virtualization.sdk.network;

import java.time.Instant;
import java.util.Objects;

/**
 * One address leased out of a {@link Subnet} by {@link IpamService#allocate}. {@code ownerRef} is
 * a free-form caller-supplied string (e.g. a VPS name) — this module never depends on {@code
 * virtualization-vps} or any other consumer, so it can't reference a typed owner id.
 */
public record IpAllocation(IpAllocationId id, SubnetId subnetId, String address, String ownerRef, Instant allocatedAt) {

    public IpAllocation {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(subnetId, "subnetId must not be null");
        Objects.requireNonNull(address, "address must not be null");
        Objects.requireNonNull(ownerRef, "ownerRef must not be null");
        Objects.requireNonNull(allocatedAt, "allocatedAt must not be null");
    }
}
