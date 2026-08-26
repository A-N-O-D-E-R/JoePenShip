package io.virtualization.sdk.dns;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class DnsRecordSpecTest {

    @Test
    void rejectsNullName() {
        assertThatNullPointerException().isThrownBy(() -> new DnsRecordSpec(null, DnsRecordType.A, "203.0.113.10", null, null));
    }

    @Test
    void rejectsBlankName() {
        assertThatIllegalArgumentException().isThrownBy(() -> new DnsRecordSpec(" ", DnsRecordType.A, "203.0.113.10", null, null));
    }

    @Test
    void rejectsNullType() {
        assertThatNullPointerException().isThrownBy(() -> new DnsRecordSpec("app", null, "203.0.113.10", null, null));
    }

    @Test
    void rejectsNullValue() {
        assertThatNullPointerException().isThrownBy(() -> new DnsRecordSpec("app", DnsRecordType.A, null, null, null));
    }

    @Test
    void rejectsBlankValue() {
        assertThatIllegalArgumentException().isThrownBy(() -> new DnsRecordSpec("app", DnsRecordType.A, " ", null, null));
    }

    @Test
    void rejectsNegativeTtl() {
        assertThatIllegalArgumentException().isThrownBy(() -> new DnsRecordSpec("app", DnsRecordType.A, "203.0.113.10", -1L, null));
    }

    @Test
    void rejectsNegativePriority() {
        assertThatIllegalArgumentException().isThrownBy(() -> new DnsRecordSpec("app", DnsRecordType.MX, "mail.example.com", null, -1));
    }

    @Test
    void allowsNullTtlAndPriority() {
        DnsRecordSpec spec = new DnsRecordSpec("app", DnsRecordType.A, "203.0.113.10", null, null);

        org.assertj.core.api.Assertions.assertThat(spec.ttl()).isNull();
        org.assertj.core.api.Assertions.assertThat(spec.priority()).isNull();
    }
}
