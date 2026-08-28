package io.virtualization.sdk.storage;

import io.virtualization.sdk.core.exception.ConfigurationException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StorageProviderRegistryTest {

    private final StorageProvider provider = new NoOpStorageProvider();

    @Test
    void getReturnsRegisteredProvider() {
        StorageProviderRegistry registry = new StorageProviderRegistry(Map.of("incus", provider));

        assertThat(registry.get("incus")).isSameAs(provider);
    }

    @Test
    void getThrowsConfigurationExceptionForUnknownName() {
        StorageProviderRegistry registry = new StorageProviderRegistry(Map.of("incus", provider));

        assertThatThrownBy(() -> registry.get("proxmox")).isInstanceOf(ConfigurationException.class);
    }

    @Test
    void allIsUnmodifiableAndComplete() {
        StorageProviderRegistry registry = new StorageProviderRegistry(Map.of("incus", provider));

        assertThat(registry.all()).containsExactly(Map.entry("incus", provider));
        assertThat(registry.all()).isUnmodifiable();
    }

    @Test
    void rejectsBlankProviderName() {
        Map<String, StorageProvider> providers = new HashMap<>();
        providers.put(" ", provider);

        assertThatIllegalArgumentException().isThrownBy(() -> new StorageProviderRegistry(providers));
    }

    @Test
    void rejectsNullProvider() {
        Map<String, StorageProvider> providers = new HashMap<>();
        providers.put("incus", null);

        assertThatNullPointerException().isThrownBy(() -> new StorageProviderRegistry(providers));
    }

    @Test
    void rejectsNullMap() {
        assertThatNullPointerException().isThrownBy(() -> new StorageProviderRegistry(null));
    }

    private static final class NoOpStorageProvider implements StorageProvider {
        @Override
        public List<StoragePool> listStoragePools() {
            return List.of();
        }

        @Override
        public StoragePool getStoragePool(String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Volume> listVolumes(String pool) {
            return List.of();
        }

        @Override
        public Volume getVolume(String pool, String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Volume createVolume(String pool, VolumeSpec spec) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteVolume(String pool, String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Volume resizeVolume(String pool, String name, String newSize) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<VolumeSnapshot> listSnapshots(String pool, String volumeName) {
            return List.of();
        }

        @Override
        public VolumeSnapshot createSnapshot(String pool, String volumeName, String snapshotName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void restoreSnapshot(String pool, String volumeName, String snapshotName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteSnapshot(String pool, String volumeName, String snapshotName) {
            throw new UnsupportedOperationException();
        }
    }
}
