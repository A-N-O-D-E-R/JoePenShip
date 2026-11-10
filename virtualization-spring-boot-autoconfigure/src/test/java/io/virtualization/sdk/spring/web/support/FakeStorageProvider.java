package io.virtualization.sdk.spring.web.support;

import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.storage.StoragePool;
import io.virtualization.sdk.storage.StorageProvider;
import io.virtualization.sdk.storage.Volume;
import io.virtualization.sdk.storage.VolumeContentType;
import io.virtualization.sdk.storage.VolumeSnapshot;
import io.virtualization.sdk.storage.VolumeSpec;
import io.virtualization.sdk.storage.VolumeType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Hand-written in-memory test double — pre-seeds a single {@code "default"} pool. */
public final class FakeStorageProvider implements StorageProvider {

    private final Map<String, StoragePool> pools = new ConcurrentHashMap<>();
    private final Map<String, Volume> volumes = new ConcurrentHashMap<>();
    private final Map<String, List<VolumeSnapshot>> snapshots = new ConcurrentHashMap<>();

    public FakeStorageProvider() {
        pools.put("default", new StoragePool("default", "dir", "", Map.of(), "Created"));
    }

    private static String volumeKey(String pool, String name) {
        return pool + "/" + name;
    }

    @Override
    public List<StoragePool> listStoragePools() {
        return List.copyOf(pools.values());
    }

    @Override
    public StoragePool getStoragePool(String name) {
        StoragePool pool = pools.get(name);
        if (pool == null) {
            throw new ResourceNotFoundException("No storage pool named '" + name + "'");
        }
        return pool;
    }

    @Override
    public List<Volume> listVolumes(String pool) {
        return volumes.values().stream().filter(v -> v.pool().equals(pool)).toList();
    }

    @Override
    public Volume getVolume(String pool, String name) {
        Volume volume = volumes.get(volumeKey(pool, name));
        if (volume == null) {
            throw new ResourceNotFoundException("No volume '" + name + "' in pool '" + pool + "'");
        }
        return volume;
    }

    @Override
    public Volume createVolume(String pool, VolumeSpec spec) {
        Volume volume = new Volume(
                pool, spec.name(), VolumeType.CUSTOM,
                spec.contentType() != null ? spec.contentType() : VolumeContentType.FILESYSTEM, spec.size(), spec.config());
        volumes.put(volumeKey(pool, spec.name()), volume);
        return volume;
    }

    @Override
    public void deleteVolume(String pool, String name) {
        volumes.remove(volumeKey(pool, name));
        snapshots.remove(volumeKey(pool, name));
    }

    @Override
    public Volume resizeVolume(String pool, String name, String newSize) {
        Volume existing = getVolume(pool, name);
        Volume resized = new Volume(pool, name, existing.type(), existing.contentType(), newSize, existing.config());
        volumes.put(volumeKey(pool, name), resized);
        return resized;
    }

    @Override
    public List<VolumeSnapshot> listSnapshots(String pool, String volumeName) {
        getVolume(pool, volumeName);
        return List.copyOf(snapshots.getOrDefault(volumeKey(pool, volumeName), List.of()));
    }

    @Override
    public VolumeSnapshot createSnapshot(String pool, String volumeName, String snapshotName) {
        getVolume(pool, volumeName);
        VolumeSnapshot snapshot = new VolumeSnapshot(pool, volumeName, snapshotName, Instant.now());
        snapshots.computeIfAbsent(volumeKey(pool, volumeName), k -> new ArrayList<>()).add(snapshot);
        return snapshot;
    }

    @Override
    public void restoreSnapshot(String pool, String volumeName, String snapshotName) {
        boolean exists = snapshots.getOrDefault(volumeKey(pool, volumeName), List.of()).stream()
                .anyMatch(s -> s.name().equals(snapshotName));
        if (!exists) {
            throw new ResourceNotFoundException("No snapshot '" + snapshotName + "' on volume '" + volumeName + "'");
        }
    }

    @Override
    public void deleteSnapshot(String pool, String volumeName, String snapshotName) {
        List<VolumeSnapshot> list = snapshots.get(volumeKey(pool, volumeName));
        boolean removed = list != null && list.removeIf(s -> s.name().equals(snapshotName));
        if (!removed) {
            throw new ResourceNotFoundException("No snapshot '" + snapshotName + "' on volume '" + volumeName + "'");
        }
    }
}
