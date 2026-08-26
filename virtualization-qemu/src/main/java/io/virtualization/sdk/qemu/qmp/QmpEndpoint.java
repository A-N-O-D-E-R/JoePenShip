package io.virtualization.sdk.qemu.qmp;

/** Where a {@link QmpClient} connects to reach QEMU's QMP control socket. */
public sealed interface QmpEndpoint permits UnixSocketEndpoint, TcpEndpoint {}
