package io.virtualization.sdk.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class DomainNamesTest {

    @Test
    void lowerCasesInput() {
        assertThat(DomainNames.normalize("EXAMPLE.com")).isEqualTo("example.com");
    }

    @Test
    void stripsExactlyOneTrailingDot() {
        assertThat(DomainNames.normalize("example.com.")).isEqualTo("example.com");
    }

    @Test
    void convertsUnicodeToPunycode() {
        assertThat(DomainNames.normalize("münchen.de")).isEqualTo("xn--mnchen-3ya.de");
    }

    @Test
    void combinesLowerCaseTrailingDotAndIdn() {
        assertThat(DomainNames.normalize("MÜNCHEN.de.")).isEqualTo("xn--mnchen-3ya.de");
    }

    @Test
    void leavesAlreadyAsciiNameUnchanged() {
        assertThat(DomainNames.normalize("app.example.com")).isEqualTo("app.example.com");
    }

    @Test
    void rejectsNull() {
        assertThatNullPointerException().isThrownBy(() -> DomainNames.normalize(null));
    }

    @Test
    void rejectsBlank() {
        assertThatIllegalArgumentException().isThrownBy(() -> DomainNames.normalize(" "));
    }

    @Test
    void rejectsDoubleTrailingDot() {
        assertThatIllegalArgumentException().isThrownBy(() -> DomainNames.normalize("example.com.."));
    }

    @Test
    void rejectsUnderscoreLabel() {
        assertThatIllegalArgumentException().isThrownBy(() -> DomainNames.normalize("_dmarc.example.com"));
    }

    @Test
    void rejectsRootOnly() {
        assertThatIllegalArgumentException().isThrownBy(() -> DomainNames.normalize("."));
    }
}
