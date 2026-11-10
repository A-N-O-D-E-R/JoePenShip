package io.virtualization.sdk.storage;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class StoragePoolTest {

    @Test
    void rejectsBlankName() {
        assertThatIllegalArgumentException().isThrownBy(() -> new StoragePool("", "zfs", null, null, "Created"));
    }

    @Test
    void requiresDriver() {
        assertThatNullPointerException().isThrownBy(() -> new StoragePool("default", null, null, null, "Created"));
    }

    @Test
    void nullConfigDefaultsToEmpty() {
        StoragePool pool = new StoragePool("default", "zfs", "desc", null, "Created");

        assertThat(pool.config()).isEmpty();
    }

    @Test
    void configIsDefensivelyCopied() {
        Map<String, String> config = new java.util.HashMap<>(Map.of("size", "50GB"));
        StoragePool pool = new StoragePool("default", "zfs", null, config, "Created");
        config.put("extra", "value");

        assertThat(pool.config()).containsOnly(Map.entry("size", "50GB"));
    }
}
