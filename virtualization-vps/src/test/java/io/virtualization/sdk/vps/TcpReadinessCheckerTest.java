package io.virtualization.sdk.vps;

import io.virtualization.sdk.core.ComputeResources;
import io.virtualization.sdk.core.image.ImageReference;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TcpReadinessCheckerTest {

    private static final ImageReference IMAGE = new ImageReference("incus", "images", "ubuntu/24.04");

    @Test
    void noKnownAddressIsAlwaysReady() {
        TcpReadinessChecker checker = new TcpReadinessChecker();

        assertThat(checker.isReady(sampleVps(NetworkConfiguration.UNSPECIFIED))).isTrue();
    }

    @Test
    void reachableAddressIsReady() throws IOException {
        try (ServerSocket listening = new ServerSocket(0)) {
            TcpReadinessChecker checker = new TcpReadinessChecker(listening.getLocalPort(), Duration.ofSeconds(1));
            NetworkConfiguration network = new NetworkConfiguration("default", "127.0.0.1", null, "web-01");

            assertThat(checker.isReady(sampleVps(network))).isTrue();
        }
    }

    @Test
    void unreachableAddressIsNotReady() throws IOException {
        int closedPort;
        try (ServerSocket probe = new ServerSocket(0)) {
            closedPort = probe.getLocalPort();
        } // closed immediately — nothing listens on it now.
        TcpReadinessChecker checker = new TcpReadinessChecker(closedPort, Duration.ofMillis(500));
        NetworkConfiguration network = new NetworkConfiguration("default", "127.0.0.1", null, "web-01");

        assertThat(checker.isReady(sampleVps(network))).isFalse();
    }

    @Test
    void rejectsOutOfRangePort() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new TcpReadinessChecker(0, Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new TcpReadinessChecker(70_000, Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Vps sampleVps(NetworkConfiguration network) {
        Instant now = Instant.now();
        VpsSpec spec = VpsSpec.builder("web-01", IMAGE).build();
        return new Vps(
                VpsId.generate(), "web-01", VpsState.PROVISIONING, VpsType.VIRTUAL_MACHINE, IMAGE,
                new ComputeResources(1, 1_024), new StorageConfiguration(DataSize.ofGigabytes(10)), network, spec,
                "incus", null, "workload-1", now, now, null, null, null);
    }
}
