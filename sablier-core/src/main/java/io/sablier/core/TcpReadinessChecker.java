package io.sablier.core;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.Objects;

/** Readiness via a raw TCP connect attempt to a fixed {@code host:port} — success means "port accepting connections". */
public final class TcpReadinessChecker implements ReadinessChecker {

    private final String host;
    private final int port;
    private final Duration connectTimeout;

    public TcpReadinessChecker(String host, int port, Duration connectTimeout) {
        this.host = Objects.requireNonNull(host, "host must not be null");
        this.connectTimeout = Objects.requireNonNull(connectTimeout, "connectTimeout must not be null");
        if (host.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("port must be within [1, 65535], was " + port);
        }
        if (connectTimeout.isNegative() || connectTimeout.isZero()) {
            throw new IllegalArgumentException("connectTimeout must be positive");
        }
        this.port = port;
    }

    @Override
    public ReadinessStatus check(Workload workload) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), Math.toIntExact(connectTimeout.toMillis()));
            return ReadinessStatus.ready();
        } catch (IOException e) {
            return ReadinessStatus.pending("TCP connect to " + host + ":" + port + " failed: " + e.getMessage());
        }
    }
}
