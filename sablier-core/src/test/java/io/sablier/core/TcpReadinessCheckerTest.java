package io.sablier.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class TcpReadinessCheckerTest {

    private static final Workload WORKLOAD =
            new Workload("w-1", "jellyfin", WorkloadType.CONTAINER, WorkloadState.RUNNING, "media", "default", Optional.empty());

    private ServerSocket serverSocket;

    @AfterEach
    void closeServer() throws IOException {
        if (serverSocket != null) {
            serverSocket.close();
        }
    }

    @Test
    void readyWhenPortAcceptsConnections() throws IOException {
        serverSocket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress());
        TcpReadinessChecker checker = new TcpReadinessChecker("localhost", serverSocket.getLocalPort(), Duration.ofSeconds(2));

        assertThat(checker.check(WORKLOAD).state()).isEqualTo(ReadinessState.READY);
    }

    @Test
    void pendingWhenNothingListening() {
        TcpReadinessChecker checker = new TcpReadinessChecker("localhost", 1, Duration.ofMillis(500));

        assertThat(checker.check(WORKLOAD).state()).isEqualTo(ReadinessState.PENDING);
    }

    @Test
    void rejectsInvalidConstructorArguments() {
        assertThatIllegalArgumentException().isThrownBy(() -> new TcpReadinessChecker("", 80, Duration.ofSeconds(1)));
        assertThatIllegalArgumentException().isThrownBy(() -> new TcpReadinessChecker("host", 0, Duration.ofSeconds(1)));
        assertThatIllegalArgumentException().isThrownBy(() -> new TcpReadinessChecker("host", 70000, Duration.ofSeconds(1)));
        assertThatIllegalArgumentException().isThrownBy(() -> new TcpReadinessChecker("host", 80, Duration.ZERO));
    }
}
