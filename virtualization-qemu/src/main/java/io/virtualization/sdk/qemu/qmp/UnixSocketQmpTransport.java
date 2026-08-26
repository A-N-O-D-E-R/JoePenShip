package io.virtualization.sdk.qemu.qmp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;

/**
 * {@link QmpTransport} over a Unix domain socket (the common case for a QEMU instance started
 * with {@code -qmp unix:/path/to/socket,server}).
 */
final class UnixSocketQmpTransport implements QmpTransport {

    private final Path socketPath;
    private SocketChannel channel;
    private BufferedReader reader;
    private OutputStream out;

    UnixSocketQmpTransport(Path socketPath) {
        this.socketPath = socketPath;
    }

    @Override
    public void connect(Duration timeout) throws IOException {
        // SocketChannel implements InterruptibleChannel, so cancelling the timeout task via
        // interrupt reliably unblocks a hung connect() here (unlike plain java.net.Socket).
        TimeoutSupport.runWithTimeout(timeout, () -> {
            channel = SocketChannel.open(UnixDomainSocketAddress.of(socketPath));
        });
        reader = new BufferedReader(new InputStreamReader(Channels.newInputStream(channel), StandardCharsets.UTF_8));
        out = Channels.newOutputStream(channel);
    }

    @Override
    public void writeLine(String json) throws IOException {
        out.write((json + "\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    @Override
    public String readLine() throws IOException {
        return reader.readLine();
    }

    @Override
    public void close() throws IOException {
        if (channel != null) {
            channel.close();
        }
    }
}
