package io.virtualization.sdk.vps;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class StorageConfigurationTest {

    @Test
    void rejectsNullOrNonPositiveRootDisk() {
        assertThatNullPointerException().isThrownBy(() -> new StorageConfiguration(null, "pool", "type"));
        assertThatIllegalArgumentException().isThrownBy(() -> new StorageConfiguration(DataSize.ofBytes(0), "pool", "type"));
    }

    @Test
    void nullablePoolAndVolumeTypeAccepted() {
        StorageConfiguration config = new StorageConfiguration(DataSize.ofGigabytes(10));

        assertThat(config.rootDisk()).isEqualTo(DataSize.ofGigabytes(10));
        assertThat(config.storagePool()).isNull();
        assertThat(config.volumeType()).isNull();
    }
}
