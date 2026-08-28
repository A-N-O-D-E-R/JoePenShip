package io.virtualization.sdk.storage;

import java.util.Map;
import java.util.Objects;

/**
 * A storage pool as reported by a {@link StorageProvider} — read-only in this SDK: pool lifecycle
 * (create/delete) stays an infrastructure-admin operation done directly against the backend, since
 * pool drivers (zfs/btrfs/dir/lvm/ceph, ...) have wildly different config requirements this SDK
 * has no way to validate. {@code status} is a raw provider string, not an enum — its real set of
 * values isn't exhaustively known here.
 */
public record StoragePool(String name, String driver, String description, Map<String, String> config, String status) {

    public StoragePool {
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        Objects.requireNonNull(driver, "driver must not be null");
        config = config == null ? Map.of() : Map.copyOf(config);
    }
}
