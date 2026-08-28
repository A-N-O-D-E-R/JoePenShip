package io.virtualization.sdk.storage;

import io.virtualization.sdk.core.exception.ResourceNotFoundException;

import java.util.List;

/**
 * A storage backend (Incus, ...). Implementations live in their own provider modules — this
 * module never depends on one. Storage pools are read-only here; only {@link VolumeType#CUSTOM}
 * volumes are created.
 */
public interface StorageProvider {

    List<StoragePool> listStoragePools();

    /** @throws ResourceNotFoundException if {@code name} is unknown */
    StoragePool getStoragePool(String name);

    /** @throws ResourceNotFoundException if {@code pool} is unknown */
    List<Volume> listVolumes(String pool);

    /** @throws ResourceNotFoundException if {@code pool} or the volume is unknown */
    Volume getVolume(String pool, String name);

    /** @throws ResourceNotFoundException if {@code pool} is unknown */
    Volume createVolume(String pool, VolumeSpec spec);

    /** @throws ResourceNotFoundException if {@code pool} or the volume is unknown */
    void deleteVolume(String pool, String name);

    /** @throws ResourceNotFoundException if {@code pool} or the volume is unknown */
    Volume resizeVolume(String pool, String name, String newSize);

    /** @throws ResourceNotFoundException if {@code pool} or the volume is unknown */
    List<VolumeSnapshot> listSnapshots(String pool, String volumeName);

    /** @throws ResourceNotFoundException if {@code pool} or the volume is unknown */
    VolumeSnapshot createSnapshot(String pool, String volumeName, String snapshotName);

    /** @throws ResourceNotFoundException if {@code pool}, the volume, or the snapshot is unknown */
    void restoreSnapshot(String pool, String volumeName, String snapshotName);

    /** @throws ResourceNotFoundException if {@code pool}, the volume, or the snapshot is unknown */
    void deleteSnapshot(String pool, String volumeName, String snapshotName);
}
