package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.storage.StorageProvider;
import io.virtualization.sdk.storage.StorageProviderRegistry;
import io.virtualization.sdk.storage.Volume;
import io.virtualization.sdk.storage.VolumeSnapshot;
import io.virtualization.sdk.storage.VolumeSpec;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * {@code /api/v1/storage/pools/{pool}/volumes} (plus nested snapshots), over {@link
 * StorageProviderRegistry}. {@code provider} selects which configured entry to use, same role as
 * {@link StoragePoolController}/{@link ImageController}.
 */
@RestController
@RequestMapping("/api/v1/storage/pools/{pool}/volumes")
public class VolumeController {

    private final StorageProviderRegistry registry;

    public VolumeController(StorageProviderRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    public List<Volume> list(@PathVariable String pool, @RequestParam String provider) {
        return provider(provider).listVolumes(pool);
    }

    @GetMapping("/{name}")
    public Volume get(@PathVariable String pool, @PathVariable String name, @RequestParam String provider) {
        return provider(provider).getVolume(pool, name);
    }

    @PostMapping
    public Volume create(@PathVariable String pool, @RequestParam String provider, @RequestBody CreateVolumeRequestBody request) {
        Objects.requireNonNull(request.name(), "'name' is required");
        Map<String, String> config = request.config() != null ? request.config() : Map.of();
        return provider(provider).createVolume(pool, new VolumeSpec(request.name(), request.contentType(), request.size(), config));
    }

    @DeleteMapping("/{name}")
    public void delete(@PathVariable String pool, @PathVariable String name, @RequestParam String provider) {
        provider(provider).deleteVolume(pool, name);
    }

    @PostMapping("/{name}/resize")
    public Volume resize(
            @PathVariable String pool, @PathVariable String name, @RequestParam String provider,
            @RequestBody ResizeVolumeRequestBody request) {
        Objects.requireNonNull(request.size(), "'size' is required");
        return provider(provider).resizeVolume(pool, name, request.size());
    }

    @GetMapping("/{name}/snapshots")
    public List<VolumeSnapshot> listSnapshots(@PathVariable String pool, @PathVariable String name, @RequestParam String provider) {
        return provider(provider).listSnapshots(pool, name);
    }

    @PostMapping("/{name}/snapshots")
    public VolumeSnapshot createSnapshot(
            @PathVariable String pool, @PathVariable String name, @RequestParam String provider,
            @RequestBody CreateVolumeSnapshotRequestBody request) {
        Objects.requireNonNull(request.name(), "'name' is required");
        return provider(provider).createSnapshot(pool, name, request.name());
    }

    @PostMapping("/{name}/snapshots/{snapshot}/restore")
    public void restoreSnapshot(
            @PathVariable String pool, @PathVariable String name, @PathVariable String snapshot, @RequestParam String provider) {
        provider(provider).restoreSnapshot(pool, name, snapshot);
    }

    @DeleteMapping("/{name}/snapshots/{snapshot}")
    public void deleteSnapshot(
            @PathVariable String pool, @PathVariable String name, @PathVariable String snapshot, @RequestParam String provider) {
        provider(provider).deleteSnapshot(pool, name, snapshot);
    }

    private StorageProvider provider(String name) {
        return registry.get(name);
    }
}
