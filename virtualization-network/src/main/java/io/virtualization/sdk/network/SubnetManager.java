package io.virtualization.sdk.network;

import io.virtualization.sdk.core.exception.ResourceNotFoundException;

import java.util.List;

public interface SubnetManager {

    /**
     * @throws ResourceNotFoundException if {@code networkId} is unknown
     * @throws IllegalArgumentException if {@code cidr} is invalid or overlaps another subnet in
     *     the same network
     */
    Subnet create(NetworkId networkId, String cidr, String gateway);

    /** @throws ResourceNotFoundException if {@code id} is unknown */
    Subnet get(SubnetId id);

    List<Subnet> list(NetworkId networkId);

    /** @throws ResourceNotFoundException if {@code id} is unknown */
    void delete(SubnetId id);
}
