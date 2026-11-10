package io.virtualization.sdk.storage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class VolumeSpecTest {

    @Test
    void rejectsBlankName() {
        assertThatIllegalArgumentException().isThrownBy(() -> new VolumeSpec("", null, null, null));
    }

    @Test
    void contentTypeAndSizeAreOptional() {
        VolumeSpec spec = new VolumeSpec("data", null, null, null);

        assertThat(spec.contentType()).isNull();
        assertThat(spec.size()).isNull();
        assertThat(spec.config()).isEmpty();
    }
}
