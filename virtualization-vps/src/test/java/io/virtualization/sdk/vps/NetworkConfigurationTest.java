package io.virtualization.sdk.vps;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NetworkConfigurationTest {

    @Test
    void unspecifiedIsAllNull() {
        assertThat(NetworkConfiguration.UNSPECIFIED.network()).isNull();
        assertThat(NetworkConfiguration.UNSPECIFIED.ipv4()).isNull();
        assertThat(NetworkConfiguration.UNSPECIFIED.ipv6()).isNull();
        assertThat(NetworkConfiguration.UNSPECIFIED.hostname()).isNull();
    }

    @Test
    void valuesRoundTrip() {
        NetworkConfiguration config = new NetworkConfiguration("default", "10.0.0.5", "fd00::5", "web-01");

        assertThat(config.network()).isEqualTo("default");
        assertThat(config.ipv4()).isEqualTo("10.0.0.5");
        assertThat(config.ipv6()).isEqualTo("fd00::5");
        assertThat(config.hostname()).isEqualTo("web-01");
    }
}
