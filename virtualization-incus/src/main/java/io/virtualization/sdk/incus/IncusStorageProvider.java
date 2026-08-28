package io.virtualization.sdk.incus;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.incus.client.IncusApiClient;
import io.virtualization.sdk.incus.client.dto.StoragePoolDto;
import io.virtualization.sdk.incus.client.dto.StorageVolumeDto;
import io.virtualization.sdk.incus.client.dto.StorageVolumeSnapshotDto;
import io.virtualization.sdk.incus.internal.SyncOperationWaiter;
import io.virtualization.sdk.storage.StoragePool;
import io.virtualization.sdk.storage.StorageProvider;
import io.virtualization.sdk.storage.Volume;
import io.virtualization.sdk.storage.VolumeContentType;
import io.virtualization.sdk.storage.VolumeSnapshot;
import io.virtualization.sdk.storage.VolumeSpec;
import io.virtualization.sdk.storage.VolumeType;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link StorageProvider} backed by the Incus REST API. Storage pools are read-only (list/get) —
 * pool lifecycle stays an infrastructure-admin operation done directly against Incus. Only {@code
 * custom} volumes are created/managed by this SDK, though {@link #listVolumes}/{@link #getVolume}
 * report every volume kind a pool holds for read-fidelity.
 *
 * <p>Reuses {@link IncusApiClient}'s existing generic HTTP methods — no client changes needed.
 * Every mutating call blocks the caller until the underlying Incus operation finishes (via {@link
 * SyncOperationWaiter}), matching this interface's synchronous contract (unlike {@link
 * IncusProvider}'s async {@code Operation}-returning VM lifecycle).
 */
public final class IncusStorageProvider implements StorageProvider {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final IncusApiClient client;
    private final Duration operationWaitTimeout;

    public IncusStorageProvider(IncusClientConfig config) {
        this(new IncusApiClient(config), config.operationWaitTimeout());
    }

    /** Visible for tests, to inject a client talking to a fake server. */
    IncusStorageProvider(IncusApiClient client, Duration operationWaitTimeout) {
        this.client = client;
        this.operationWaitTimeout = operationWaitTimeout;
    }

    @Override
    public List<StoragePool> listStoragePools() {
        return client.getList("/storage-pools?recursion=1", StoragePoolDto.class).stream().map(IncusStorageProvider::toPool).toList();
    }

    @Override
    public StoragePool getStoragePool(String name) {
        return toPool(client.getSingle("/storage-pools/" + name, StoragePoolDto.class));
    }

    @Override
    public List<Volume> listVolumes(String pool) {
        return client.getList("/storage-pools/" + pool + "/volumes?recursion=1", StorageVolumeDto.class).stream()
                .map(dto -> toVolume(pool, dto))
                .toList();
    }

    @Override
    public Volume getVolume(String pool, String name) {
        return toVolume(pool, client.getSingle(volumePath(pool, name), StorageVolumeDto.class));
    }

    @Override
    public Volume createVolume(String pool, VolumeSpec spec) {
        Map<String, String> config = new LinkedHashMap<>(spec.config());
        if (spec.size() != null) {
            config.put("size", spec.size());
        }
        String contentType = spec.contentType() != null ? toContentTypeString(spec.contentType()) : null;
        String operationId = client.postForOperationId("/storage-pools/" + pool + "/volumes", writeJson(
                new VolumeCreateDto(spec.name(), "custom", contentType, config)));
        if (operationId != null) {
            SyncOperationWaiter.await(client, operationId, operationWaitTimeout);
        }
        return getVolume(pool, spec.name());
    }

    @Override
    public void deleteVolume(String pool, String name) {
        String operationId = client.deleteForOperationId(volumePath(pool, name));
        if (operationId != null) {
            SyncOperationWaiter.await(client, operationId, operationWaitTimeout);
        }
    }

    /** Incus's {@code PUT} replaces the whole volume resource — get-then-merge-then-put, not a shortcut. */
    @Override
    public Volume resizeVolume(String pool, String name, String newSize) {
        StorageVolumeDto current = client.getSingle(volumePath(pool, name), StorageVolumeDto.class);
        Map<String, String> config = current.config() != null ? new LinkedHashMap<>(current.config()) : new LinkedHashMap<>();
        config.put("size", newSize);
        String operationId = client.putForOperationId(volumePath(pool, name), writeJson(new VolumeConfigUpdateDto(config)));
        if (operationId != null) {
            SyncOperationWaiter.await(client, operationId, operationWaitTimeout);
        }
        return getVolume(pool, name);
    }

    @Override
    public List<VolumeSnapshot> listSnapshots(String pool, String volumeName) {
        return client.getList(volumePath(pool, volumeName) + "/snapshots?recursion=1", StorageVolumeSnapshotDto.class).stream()
                .map(dto -> toSnapshot(pool, volumeName, dto))
                .toList();
    }

    @Override
    public VolumeSnapshot createSnapshot(String pool, String volumeName, String snapshotName) {
        String operationId = client.postForOperationId(
                volumePath(pool, volumeName) + "/snapshots", writeJson(new SnapshotCreateDto(snapshotName)));
        if (operationId != null) {
            SyncOperationWaiter.await(client, operationId, operationWaitTimeout);
        }
        return listSnapshots(pool, volumeName).stream()
                .filter(s -> s.name().equals(snapshotName))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Snapshot '" + snapshotName + "' not found on volume '" + volumeName + "' after creation"));
    }

    /** Incus's real restore mechanism — a {@code PUT} on the volume with a {@code restore} field, not a call on the snapshot. */
    @Override
    public void restoreSnapshot(String pool, String volumeName, String snapshotName) {
        String operationId = client.putForOperationId(volumePath(pool, volumeName), writeJson(new VolumeRestoreDto(snapshotName)));
        if (operationId != null) {
            SyncOperationWaiter.await(client, operationId, operationWaitTimeout);
        }
    }

    @Override
    public void deleteSnapshot(String pool, String volumeName, String snapshotName) {
        String operationId = client.deleteForOperationId(volumePath(pool, volumeName) + "/snapshots/" + snapshotName);
        if (operationId != null) {
            SyncOperationWaiter.await(client, operationId, operationWaitTimeout);
        }
    }

    private static String volumePath(String pool, String name) {
        return "/storage-pools/" + pool + "/volumes/custom/" + name;
    }

    private static StoragePool toPool(StoragePoolDto dto) {
        return new StoragePool(dto.name(), dto.driver(), dto.description(), dto.config(), dto.status());
    }

    private static Volume toVolume(String pool, StorageVolumeDto dto) {
        String size = dto.config() != null ? dto.config().get("size") : null;
        return new Volume(pool, dto.name(), toVolumeType(dto.type()), toContentType(dto.contentType()), size, dto.config());
    }

    private static VolumeSnapshot toSnapshot(String pool, String volumeName, StorageVolumeSnapshotDto dto) {
        return new VolumeSnapshot(pool, volumeName, dto.name(), parseInstantOrNull(dto.createdAt()));
    }

    private static VolumeType toVolumeType(String raw) {
        return switch (raw) {
            case "custom" -> VolumeType.CUSTOM;
            case "container" -> VolumeType.CONTAINER;
            case "virtual-machine" -> VolumeType.VIRTUAL_MACHINE;
            case "image" -> VolumeType.IMAGE;
            default -> throw new IllegalStateException("Unknown Incus volume type '" + raw + "'");
        };
    }

    private static VolumeContentType toContentType(String raw) {
        return switch (raw) {
            case "filesystem" -> VolumeContentType.FILESYSTEM;
            case "block" -> VolumeContentType.BLOCK;
            default -> throw new IllegalStateException("Unknown Incus volume content type '" + raw + "'");
        };
    }

    private static String toContentTypeString(VolumeContentType contentType) {
        return switch (contentType) {
            case FILESYSTEM -> "filesystem";
            case BLOCK -> "block";
        };
    }

    private static Instant parseInstantOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(raw);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String writeJson(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unreachable: storage request body always serializes", e);
        }
    }

    private record VolumeCreateDto(String name, String type, @JsonProperty("content_type") String contentType, Map<String, String> config) {}

    private record VolumeConfigUpdateDto(Map<String, String> config) {}

    private record VolumeRestoreDto(String restore) {}

    private record SnapshotCreateDto(String name) {}
}
