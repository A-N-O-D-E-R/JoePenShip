package io.virtualization.sdk.storage;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class VolumeTest {

    @Test
    void requiresPoolNameTypeAndContentType() {
        assertThatNullPointerException().isThrownBy(() -> new Volume(null, "data", VolumeType.CUSTOM, VolumeContentType.FILESYSTEM, null, null));
        assertThatNullPointerException().isThrownBy(() -> new Volume("default", null, VolumeType.CUSTOM, VolumeContentType.FILESYSTEM, null, null));
        assertThatNullPointerException().isThrownBy(() -> new Volume("default", "data", null, VolumeContentType.FILESYSTEM, null, null));
        assertThatNullPointerException().isThrownBy(() -> new Volume("default", "data", VolumeType.CUSTOM, null, null, null));
    }

    @Test
    void nullSizeMeansPoolDefault() {
        Volume volume = new Volume("default", "data", VolumeType.CUSTOM, VolumeContentType.FILESYSTEM, null, Map.of());

        assertThat(volume.size()).isNull();
    }

    @Test
    void nullConfigDefaultsToEmpty() {
        Volume volume = new Volume("default", "data", VolumeType.CUSTOM, VolumeContentType.FILESYSTEM, "10GB", null);

        assertThat(volume.config()).isEmpty();
    }
}
