package io.virtualization.sdk.qemu.qmp;

import java.util.Objects;

/** A QMP endpoint reachable over TCP, e.g. QEMU started with {@code -qmp tcp:host:port,server}. */
public record TcpEndpoint(String host, int port) implements QmpEndpoint {

    public TcpEndpoint {
        Objects.requireNonNull(host, "host must not be null");
        if (host.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("port must be within [1, 65535], was " + port);
        }
    }
}
