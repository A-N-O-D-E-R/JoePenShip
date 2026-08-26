package io.virtualization.sdk.vps;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.Objects;

/**
 * Probes readiness with a plain TCP connect to {@link NetworkConfiguration#ipv4()} (falling back
 * to {@link NetworkConfiguration#ipv6()}) on {@link #port}, e.g. port 22 for SSH reachability. A
 * VPS with no known static address (pure DHCP) can't be probed this way — {@link #isReady}
 * returns {@code true} for it, since there's no address to connect to yet.
 */
public final class TcpReadinessChecker implements VpsReadinessChecker {

    private final int port;
    private final Duration timeout;

    public TcpReadinessChecker(int port, Duration timeout) {
        if (port < 1 || port > 65_535) {
            throw new IllegalArgumentException("port must be between 1 and 65535, was " + port);
        }
        this.port = port;
        this.timeout = Objects.requireNonNull(timeout, "timeout must not be null");
    }

    /** Defaults to port 22 (SSH), 2s connect timeout. */
    public TcpReadinessChecker() {
        this(22, Duration.ofSeconds(2));
    }

    @Override
    public boolean isReady(Vps vps) {
        String host = vps.network().ipv4() != null ? vps.network().ipv4() : vps.network().ipv6();
        if (host == null) {
            return true;
        }
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), (int) timeout.toMillis());
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
