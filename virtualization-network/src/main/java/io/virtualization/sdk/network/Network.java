package io.virtualization.sdk.network;

import java.time.Instant;
import java.util.Objects;

/**
 * A provider-neutral, named grouping of {@link Subnet}s and {@link FirewallRule}s — an isolated
 * L2 domain in the sense that two different {@code Network}s may legitimately reuse the same CIDR
 * range. {@code name} is unique (enforced by {@link DefaultNetworkManager#create}), {@code
 * description} is optional free text.
 */
public record Network(NetworkId id, String name, String description, Instant createdAt) {

    public Network {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }
}
