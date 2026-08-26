package io.virtualization.sdk.dns;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class DnsZoneTest {

    @Test
    void rejectsNullName() {
        assertThatNullPointerException().isThrownBy(() -> new DnsZone(null, "cloudflare", "zone-1"));
    }

    @Test
    void rejectsBlankName() {
        assertThatIllegalArgumentException().isThrownBy(() -> new DnsZone(" ", "cloudflare", "zone-1"));
    }

    @Test
    void rejectsNullProvider() {
        assertThatNullPointerException().isThrownBy(() -> new DnsZone("example.com", null, "zone-1"));
    }

    @Test
    void rejectsBlankProvider() {
        assertThatIllegalArgumentException().isThrownBy(() -> new DnsZone("example.com", " ", "zone-1"));
    }

    @Test
    void rejectsNullProviderId() {
        assertThatNullPointerException().isThrownBy(() -> new DnsZone("example.com", "cloudflare", null));
    }

    @Test
    void rejectsBlankProviderId() {
        assertThatIllegalArgumentException().isThrownBy(() -> new DnsZone("example.com", "cloudflare", " "));
    }
}
