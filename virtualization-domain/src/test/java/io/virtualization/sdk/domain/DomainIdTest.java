package io.virtualization.sdk.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class DomainIdTest {

    @Test
    void generateProducesUniqueNonBlankValues() {
        DomainId a = DomainId.generate();
        DomainId b = DomainId.generate();

        assertThat(a.value()).isNotBlank();
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void rejectsNullOrBlank() {
        assertThatNullPointerException().isThrownBy(() -> new DomainId(null));
        assertThatIllegalArgumentException().isThrownBy(() -> new DomainId(""));
    }
}
