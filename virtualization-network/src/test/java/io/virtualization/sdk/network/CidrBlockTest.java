package io.virtualization.sdk.network;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CidrBlockTest {

    @Test
    void parseRejectsAMissingPrefix() {
        assertThatThrownBy(() -> CidrBlock.parse("10.0.0.0")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseRejectsANonNumericPrefix() {
        assertThatThrownBy(() -> CidrBlock.parse("10.0.0.0/abc")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseRejectsAnOutOfRangeIpv4Prefix() {
        assertThatThrownBy(() -> CidrBlock.parse("10.0.0.0/33")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void parseRejectsAnUnparseableAddress() {
        assertThatThrownBy(() -> CidrBlock.parse("not-an-ip/24")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void networkAndBroadcastAddressesAreComputedFromThePrefix() {
        CidrBlock block = CidrBlock.parse("10.0.0.50/24");

        assertThat(block.networkAddress()).isEqualTo("10.0.0.0");
        assertThat(block.broadcastAddress()).isEqualTo("10.0.0.255");
    }

    @Test
    void containsMatchesAddressesInTheBlockOnly() {
        CidrBlock block = CidrBlock.parse("10.0.0.0/24");

        assertThat(block.contains("10.0.0.1")).isTrue();
        assertThat(block.contains("10.0.0.255")).isTrue();
        assertThat(block.contains("10.0.1.1")).isFalse();
    }

    @Test
    void overlapsDetectsOverlappingAndNonOverlappingBlocks() {
        CidrBlock a = CidrBlock.parse("10.0.0.0/24");
        CidrBlock b = CidrBlock.parse("10.0.0.128/25");
        CidrBlock c = CidrBlock.parse("10.0.1.0/24");

        assertThat(a.overlaps(b)).isTrue();
        assertThat(b.overlaps(a)).isTrue();
        assertThat(a.overlaps(c)).isFalse();
    }

    @Test
    void usableHostAddressesExcludesNetworkAndBroadcastFor24() {
        CidrBlock block = CidrBlock.parse("10.0.0.0/30");

        List<String> hosts = drain(block);

        assertThat(hosts).containsExactly("10.0.0.1", "10.0.0.2");
    }

    @Test
    void usableHostAddressesIncludesBothAddressesFor31() {
        CidrBlock block = CidrBlock.parse("10.0.0.0/31");

        List<String> hosts = drain(block);

        assertThat(hosts).containsExactly("10.0.0.0", "10.0.0.1");
    }

    @Test
    void usableHostAddressesIsJustTheSingleAddressFor32() {
        CidrBlock block = CidrBlock.parse("10.0.0.5/32");

        List<String> hosts = drain(block);

        assertThat(hosts).containsExactly("10.0.0.5");
    }

    @Test
    void ipv6BlockRejectsAddressMathOperations() {
        CidrBlock block = CidrBlock.parse("fd00::/64");

        assertThat(block.isIpv4()).isFalse();
        assertThatThrownBy(block::networkAddress).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(block::usableHostAddresses).isInstanceOf(UnsupportedOperationException.class);
    }

    private static List<String> drain(CidrBlock block) {
        List<String> addresses = new ArrayList<>();
        block.usableHostAddresses().forEachRemaining(addresses::add);
        return addresses;
    }
}
