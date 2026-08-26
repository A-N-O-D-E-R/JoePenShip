package io.virtualization.sdk.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class DomainTest {

    private static final DomainId ID = DomainId.generate();
    private static final Instant NOW = Instant.now();

    @Test
    void rejectsNullName() {
        assertThatNullPointerException().isThrownBy(() -> new Domain(ID, null, DomainStatus.ACTIVE, null, NOW));
    }

    @Test
    void rejectsBlankName() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Domain(ID, " ", DomainStatus.ACTIVE, null, NOW));
    }

    @Test
    void rejectsUppercaseName() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Domain(ID, "Example.com", DomainStatus.ACTIVE, null, NOW));
    }

    @Test
    void rejectsTrailingDotName() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Domain(ID, "example.com.", DomainStatus.ACTIVE, null, NOW));
    }

    @Test
    void rejectsNullId() {
        assertThatNullPointerException().isThrownBy(() -> new Domain(null, "example.com", DomainStatus.ACTIVE, null, NOW));
    }

    @Test
    void rejectsNullStatus() {
        assertThatNullPointerException().isThrownBy(() -> new Domain(ID, "example.com", null, null, NOW));
    }

    @Test
    void rejectsNullCreatedAt() {
        assertThatNullPointerException().isThrownBy(() -> new Domain(ID, "example.com", DomainStatus.ACTIVE, null, null));
    }

    @Test
    void allowsNullDnsProvider() {
        Domain domain = new Domain(ID, "example.com", DomainStatus.ACTIVE, null, NOW);

        assertThat(domain.dnsProvider()).isNull();
    }
}
