package io.virtualization.sdk.incus;

import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.incus.client.IncusApiClient;
import io.virtualization.sdk.incus.support.FakeIncusServer;
import io.virtualization.sdk.storage.StoragePool;
import io.virtualization.sdk.storage.Volume;
import io.virtualization.sdk.storage.VolumeContentType;
import io.virtualization.sdk.storage.VolumeSnapshot;
import io.virtualization.sdk.storage.VolumeSpec;
import io.virtualization.sdk.storage.VolumeType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IncusStorageProviderTest {

    private FakeIncusServer server;
    private IncusStorageProvider provider;

    @BeforeEach
    void startServer() {
        server = new FakeIncusServer().start();
        IncusApiClient client = new IncusApiClient(HttpClient.newHttpClient(), server.uri(), Duration.ofSeconds(5));
        provider = new IncusStorageProvider(client, Duration.ofSeconds(5));
    }

    @AfterEach
    void stopServer() {
        server.close();
    }

    @Test
    void listStoragePoolsReturnsTheSeededDefaultPool() {
        List<StoragePool> pools = provider.listStoragePools();

        assertThat(pools).hasSize(1);
        assertThat(pools.get(0).name()).isEqualTo("default");
        assertThat(pools.get(0).driver()).isEqualTo("dir");
    }

    @Test
    void getStoragePoolReturnsTheNamedPool() {
        StoragePool pool = provider.getStoragePool("default");

        assertThat(pool.status()).isEqualTo("Created");
    }

    @Test
    void getStoragePoolThrowsResourceNotFoundForUnknownPool() {
        assertThatThrownBy(() -> provider.getStoragePool("missing")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createVolumeThenGetRoundTrips() {
        Volume created = provider.createVolume("default", new VolumeSpec("data", VolumeContentType.FILESYSTEM, "10GB", Map.of()));

        assertThat(created.name()).isEqualTo("data");
        assertThat(created.pool()).isEqualTo("default");
        assertThat(created.type()).isEqualTo(VolumeType.CUSTOM);
        assertThat(created.contentType()).isEqualTo(VolumeContentType.FILESYSTEM);
        assertThat(created.size()).isEqualTo("10GB");

        assertThat(provider.getVolume("default", "data")).isEqualTo(created);
    }

    @Test
    void listVolumesReturnsEveryVolumeInThePool() {
        provider.createVolume("default", new VolumeSpec("data", VolumeContentType.FILESYSTEM, "10GB", Map.of()));

        assertThat(provider.listVolumes("default")).extracting(Volume::name).containsExactly("data");
    }

    @Test
    void getVolumeThrowsResourceNotFoundForUnknownVolume() {
        assertThatThrownBy(() -> provider.getVolume("default", "missing")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteVolumeRemovesIt() {
        provider.createVolume("default", new VolumeSpec("data", VolumeContentType.FILESYSTEM, "10GB", Map.of()));

        provider.deleteVolume("default", "data");

        assertThatThrownBy(() -> provider.getVolume("default", "data")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void resizeVolumePreservesOtherConfigKeys() {
        provider.createVolume("default", new VolumeSpec("data", VolumeContentType.FILESYSTEM, "10GB", Map.of("block.filesystem", "ext4")));

        Volume resized = provider.resizeVolume("default", "data", "20GB");

        assertThat(resized.size()).isEqualTo("20GB");
        assertThat(resized.config()).containsEntry("block.filesystem", "ext4");
    }

    @Test
    void createListRestoreAndDeleteSnapshot() {
        provider.createVolume("default", new VolumeSpec("data", VolumeContentType.FILESYSTEM, "10GB", Map.of()));

        VolumeSnapshot snapshot = provider.createSnapshot("default", "data", "before-upgrade");
        assertThat(snapshot.name()).isEqualTo("before-upgrade");
        assertThat(provider.listSnapshots("default", "data")).extracting(VolumeSnapshot::name).containsExactly("before-upgrade");

        provider.restoreSnapshot("default", "data", "before-upgrade");

        provider.deleteSnapshot("default", "data", "before-upgrade");
        assertThat(provider.listSnapshots("default", "data")).isEmpty();
    }

    @Test
    void restoreSnapshotThrowsResourceNotFoundForUnknownSnapshot() {
        provider.createVolume("default", new VolumeSpec("data", VolumeContentType.FILESYSTEM, "10GB", Map.of()));

        assertThatThrownBy(() -> provider.restoreSnapshot("default", "data", "missing")).isInstanceOf(ResourceNotFoundException.class);
    }
}
