package io.virtualization.sdk.vps;

import java.util.Objects;

/**
 * A VPS's root disk. {@code storagePool}/{@code volumeType} are nullable — the provider picks a
 * default when absent.
 */
public record StorageConfiguration(DataSize rootDisk, String storagePool, String volumeType) {

    public StorageConfiguration {
        Objects.requireNonNull(rootDisk, "rootDisk must not be null");
        if (rootDisk.bytes() <= 0) {
            throw new IllegalArgumentException("rootDisk must be positive, was " + rootDisk.bytes() + " bytes");
        }
    }

    public StorageConfiguration(DataSize rootDisk) {
        this(rootDisk, null, null);
    }
}
