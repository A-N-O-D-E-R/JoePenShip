package io.virtualization.sdk.loadbalancer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class BackendTest {

    @Test
    void rejectsBlankHost() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Backend("", 8080));
    }

    @Test
    void rejectsOutOfRangePort() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Backend("10.0.0.1", 0));
        assertThatIllegalArgumentException().isThrownBy(() -> new Backend("10.0.0.1", 70000));
    }
}
