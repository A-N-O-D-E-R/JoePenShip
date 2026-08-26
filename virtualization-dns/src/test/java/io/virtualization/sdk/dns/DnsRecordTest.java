package io.virtualization.sdk.dns;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class DnsRecordTest {

    @Test
    void rejectsNullId() {
        assertThatNullPointerException().isThrownBy(
                () -> new DnsRecord(null, "example.com", "app", DnsRecordType.A, "203.0.113.10", null, null));
    }

    @Test
    void rejectsBlankId() {
        assertThatIllegalArgumentException().isThrownBy(
                () -> new DnsRecord(" ", "example.com", "app", DnsRecordType.A, "203.0.113.10", null, null));
    }

    @Test
    void rejectsNullZone() {
        assertThatNullPointerException().isThrownBy(
                () -> new DnsRecord("rec-1", null, "app", DnsRecordType.A, "203.0.113.10", null, null));
    }

    @Test
    void rejectsBlankZone() {
        assertThatIllegalArgumentException().isThrownBy(
                () -> new DnsRecord("rec-1", " ", "app", DnsRecordType.A, "203.0.113.10", null, null));
    }

    @Test
    void rejectsNullName() {
        assertThatNullPointerException().isThrownBy(
                () -> new DnsRecord("rec-1", "example.com", null, DnsRecordType.A, "203.0.113.10", null, null));
    }

    @Test
    void rejectsNullType() {
        assertThatNullPointerException().isThrownBy(
                () -> new DnsRecord("rec-1", "example.com", "app", null, "203.0.113.10", null, null));
    }

    @Test
    void rejectsNullValue() {
        assertThatNullPointerException().isThrownBy(
                () -> new DnsRecord("rec-1", "example.com", "app", DnsRecordType.A, null, null, null));
    }

    @Test
    void rejectsNegativeTtl() {
        assertThatIllegalArgumentException().isThrownBy(
                () -> new DnsRecord("rec-1", "example.com", "app", DnsRecordType.A, "203.0.113.10", -1L, null));
    }

    @Test
    void rejectsNegativePriority() {
        assertThatIllegalArgumentException().isThrownBy(
                () -> new DnsRecord("rec-1", "example.com", "mail", DnsRecordType.MX, "mail.example.com", null, -1));
    }
}
