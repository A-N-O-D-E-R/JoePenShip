package io.virtualization.sdk.vps;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class DataSizeTest {

    @Test
    void factoriesAgreeOnMegabytes() {
        assertThat(DataSize.ofGigabytes(1).toMegabytes()).isEqualTo(1_024);
        assertThat(DataSize.ofMegabytes(512).toMegabytes()).isEqualTo(512);
        assertThat(DataSize.ofKilobytes(1_024 * 1_024).toMegabytes()).isEqualTo(1_024);
        assertThat(DataSize.ofBytes(1_024 * 1_024).toMegabytes()).isEqualTo(1);
    }

    @Test
    void negativeBytesRejected() {
        assertThatIllegalArgumentException().isThrownBy(() -> new DataSize(-1));
    }
}
