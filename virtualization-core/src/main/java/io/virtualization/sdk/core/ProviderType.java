package io.virtualization.sdk.core;

import java.util.Objects;

/**
 * Identifies the kind of backend a {@link VirtualizationProvider} talks to (e.g. {@code "proxmox"},
 * {@code "incus"}, {@code "qemu"}).
 *
 * <p>Deliberately not an enum: {@code virtualization-core} has no compile-time dependency on any
 * provider module, so provider modules each define their own constant instead of core enumerating
 * every backend that will ever exist.
 */
public record ProviderType(String id) {

    public ProviderType {
        Objects.requireNonNull(id, "id must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
    }
}
