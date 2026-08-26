package io.virtualization.sdk.qemu;

import io.virtualization.sdk.qemu.qmp.QmpEndpoint;
import io.virtualization.sdk.qemu.qmp.TcpEndpoint;
import io.virtualization.sdk.qemu.qmp.UnixSocketEndpoint;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

/**
 * Connection settings for a {@link QemuProvider}.
 *
 * <p>Unlike Proxmox and Incus, a single QMP socket controls exactly one running QEMU process —
 * there is no cluster or listing API. {@code vmId} is a name the caller assigns to identify that
 * one VM through the {@link io.virtualization.sdk.core.VirtualizationProvider} interface.
 *
 * @param vmId           caller-assigned identifier for the VM behind this QMP socket
 * @param endpoint       Unix domain socket or TCP address of the QMP control socket
 * @param connectTimeout socket connect timeout
 * @param commandTimeout per-command timeout while awaiting a QMP response
 */
public record QemuClientConfig(String vmId, QmpEndpoint endpoint, Duration connectTimeout, Duration commandTimeout) {

    public QemuClientConfig {
        Objects.requireNonNull(vmId, "vmId must not be null");
        Objects.requireNonNull(endpoint, "endpoint must not be null");
        Objects.requireNonNull(connectTimeout, "connectTimeout must not be null");
        Objects.requireNonNull(commandTimeout, "commandTimeout must not be null");
        if (vmId.isBlank()) {
            throw new IllegalArgumentException("vmId must not be blank");
        }
        if (connectTimeout.isNegative() || connectTimeout.isZero()) {
            throw new IllegalArgumentException("connectTimeout must be positive");
        }
        if (commandTimeout.isNegative() || commandTimeout.isZero()) {
            throw new IllegalArgumentException("commandTimeout must be positive");
        }
    }

    public static QemuClientConfig unixSocket(String vmId, Path socketPath) {
        return new QemuClientConfig(vmId, new UnixSocketEndpoint(socketPath), Duration.ofSeconds(5), Duration.ofSeconds(10));
    }

    public static QemuClientConfig tcp(String vmId, String host, int port) {
        return new QemuClientConfig(vmId, new TcpEndpoint(host, port), Duration.ofSeconds(5), Duration.ofSeconds(10));
    }
}
