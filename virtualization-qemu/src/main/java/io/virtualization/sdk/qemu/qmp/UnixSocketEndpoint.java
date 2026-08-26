package io.virtualization.sdk.qemu.qmp;

import java.nio.file.Path;
import java.util.Objects;

/** A QMP endpoint reachable over a Unix domain socket, e.g. {@code /run/qemu/myvm.qmp}. */
public record UnixSocketEndpoint(Path path) implements QmpEndpoint {

    public UnixSocketEndpoint {
        Objects.requireNonNull(path, "path must not be null");
    }
}
