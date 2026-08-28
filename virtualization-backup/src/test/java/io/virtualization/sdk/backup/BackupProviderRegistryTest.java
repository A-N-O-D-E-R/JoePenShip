package io.virtualization.sdk.backup;

import io.virtualization.sdk.core.exception.ConfigurationException;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BackupProviderRegistryTest {

    private final BackupProvider provider = new NoOpBackupProvider();

    @Test
    void getReturnsRegisteredProvider() {
        BackupProviderRegistry registry = new BackupProviderRegistry(Map.of("incus", provider));

        assertThat(registry.get("incus")).isSameAs(provider);
    }

    @Test
    void getThrowsConfigurationExceptionForUnknownName() {
        BackupProviderRegistry registry = new BackupProviderRegistry(Map.of("incus", provider));

        assertThatThrownBy(() -> registry.get("proxmox")).isInstanceOf(ConfigurationException.class);
    }

    @Test
    void allIsUnmodifiableAndComplete() {
        BackupProviderRegistry registry = new BackupProviderRegistry(Map.of("incus", provider));

        assertThat(registry.all()).containsExactly(Map.entry("incus", provider));
        assertThat(registry.all()).isUnmodifiable();
    }

    @Test
    void rejectsBlankProviderName() {
        Map<String, BackupProvider> providers = new HashMap<>();
        providers.put(" ", provider);

        assertThatIllegalArgumentException().isThrownBy(() -> new BackupProviderRegistry(providers));
    }

    @Test
    void rejectsNullProvider() {
        Map<String, BackupProvider> providers = new HashMap<>();
        providers.put("incus", null);

        assertThatNullPointerException().isThrownBy(() -> new BackupProviderRegistry(providers));
    }

    @Test
    void rejectsNullMap() {
        assertThatNullPointerException().isThrownBy(() -> new BackupProviderRegistry(null));
    }

    private static final class NoOpBackupProvider implements BackupProvider {
        @Override
        public List<Snapshot> listSnapshots(String instanceId) {
            return List.of();
        }

        @Override
        public Snapshot createSnapshot(String instanceId, String name, boolean stateful) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void restoreSnapshot(String instanceId, String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteSnapshot(String instanceId, String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Backup> listBackups(String instanceId) {
            return List.of();
        }

        @Override
        public Backup createBackup(String instanceId, String name, Instant expiresAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public InputStream downloadBackup(String instanceId, String name) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteBackup(String instanceId, String name) {
            throw new UnsupportedOperationException();
        }
    }
}
