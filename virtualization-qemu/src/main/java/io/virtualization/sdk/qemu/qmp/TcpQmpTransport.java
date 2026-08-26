package io.virtualization.sdk.qemu.qmp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** {@link QmpTransport} over TCP (QEMU started with {@code -qmp tcp:host:port,server}). */
final class TcpQmpTransport implements QmpTransport {

    private final String host;
    private final int port;
    private Socket socket;
    private BufferedReader reader;
    private OutputStream out;

    TcpQmpTransport(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    public void connect(Duration timeout) throws IOException {
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), Math.toIntExact(timeout.toMillis()));
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        out = socket.getOutputStream();
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
        if (socket != null) {
            socket.close();
        }
    }
}
