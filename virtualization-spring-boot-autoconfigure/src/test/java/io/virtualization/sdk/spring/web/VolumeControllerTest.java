package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.spring.web.support.FakeStorageProvider;
import io.virtualization.sdk.storage.StorageProviderRegistry;
import io.virtualization.sdk.storage.Volume;
import io.virtualization.sdk.storage.VolumeContentType;
import io.virtualization.sdk.storage.VolumeSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VolumeControllerTest {

    private VolumeController controller;

    @BeforeEach
    void setUp() {
        StorageProviderRegistry registry = new StorageProviderRegistry(Map.of("fake", new FakeStorageProvider()));
        controller = new VolumeController(registry);
    }

    @Test
    void createReturnsTheCreatedVolume() {
        Volume created = controller.create("default", "fake", new CreateVolumeRequestBody("data", VolumeContentType.FILESYSTEM, "10GB", Map.of()));

        assertThat(created.name()).isEqualTo("data");
        assertThat(created.size()).isEqualTo("10GB");
    }

    @Test
    void getReturnsTheCreatedVolume() {
        controller.create("default", "fake", new CreateVolumeRequestBody("data", null, null, null));

        Volume found = controller.get("default", "data", "fake");

        assertThat(found.name()).isEqualTo("data");
    }

    @Test
    void getUnknownVolumeThrowsResourceNotFound() {
        assertThatThrownBy(() -> controller.get("default", "missing", "fake")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listReturnsEveryCreatedVolume() {
        controller.create("default", "fake", new CreateVolumeRequestBody("data", null, null, null));

        assertThat(controller.list("default", "fake")).extracting(Volume::name).containsExactly("data");
    }

    @Test
    void deleteRemovesTheVolume() {
        controller.create("default", "fake", new CreateVolumeRequestBody("data", null, null, null));

        controller.delete("default", "data", "fake");

        assertThat(controller.list("default", "fake")).isEmpty();
    }

    @Test
    void resizeChangesTheSize() {
        controller.create("default", "fake", new CreateVolumeRequestBody("data", null, "10GB", null));

        Volume resized = controller.resize("default", "data", "fake", new ResizeVolumeRequestBody("20GB"));

        assertThat(resized.size()).isEqualTo("20GB");
    }

    @Test
    void createListRestoreAndDeleteSnapshot() {
        controller.create("default", "fake", new CreateVolumeRequestBody("data", null, null, null));

        VolumeSnapshot snapshot = controller.createSnapshot("default", "data", "fake", new CreateVolumeSnapshotRequestBody("before-upgrade"));
        assertThat(snapshot.name()).isEqualTo("before-upgrade");

        List<VolumeSnapshot> snapshots = controller.listSnapshots("default", "data", "fake");
        assertThat(snapshots).extracting(VolumeSnapshot::name).containsExactly("before-upgrade");

        controller.restoreSnapshot("default", "data", "before-upgrade", "fake");

        controller.deleteSnapshot("default", "data", "before-upgrade", "fake");
        assertThat(controller.listSnapshots("default", "data", "fake")).isEmpty();
    }
}
