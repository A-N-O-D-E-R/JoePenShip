package io.virtualization.sdk.vps;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class VpsIdTest {

    @Test
    void generateProducesUniqueNonBlankValues() {
        VpsId a = VpsId.generate();
        VpsId b = VpsId.generate();

        assertThat(a.value()).isNotBlank();
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void rejectsNullOrBlank() {
        assertThatNullPointerException().isThrownBy(() -> new VpsId(null));
        assertThatIllegalArgumentException().isThrownBy(() -> new VpsId(""));
    }
}
