package io.virtualization.sdk.qemu.qmp;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.Channels;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises {@link UnixSocketQmpTransport} against a real AF_UNIX socket — the full QMP protocol
 * handling is covered separately by {@link QmpClientTest} over TCP; this just proves the
 * transport's own write/read plumbing works over a Unix domain socket.
 */
class UnixSocketQmpTransportTest {

    @Test
    void writeAndReadRoundTripOverUnixSocket() throws Exception {
        Path socketPath = Files.createTempFile("qmp-test", ".sock");
        Files.delete(socketPath);
        UnixDomainSocketAddress address = UnixDomainSocketAddress.of(socketPath);

        try (ServerSocketChannel serverChannel = ServerSocketChannel.open(StandardProtocolFamily.UNIX)) {
            serverChannel.bind(address);
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            Future<String> serverReceived = executor.submit(() -> {
                SocketChannel accepted = serverChannel.accept();
                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(Channels.newInputStream(accepted), StandardCharsets.UTF_8));
                String line = reader.readLine();
                OutputStream out = Channels.newOutputStream(accepted);
                out.write((line + "\n").getBytes(StandardCharsets.UTF_8));
                out.flush();
                return line;
            });

            UnixSocketQmpTransport transport = new UnixSocketQmpTransport(socketPath);
            transport.connect(Duration.ofSeconds(2));
            transport.writeLine("{\"hello\":\"world\"}");

            assertThat(serverReceived.get(5, TimeUnit.SECONDS)).isEqualTo("{\"hello\":\"world\"}");
            assertThat(transport.readLine()).isEqualTo("{\"hello\":\"world\"}");

            transport.close();
            executor.shutdownNow();
        } finally {
            Files.deleteIfExists(socketPath);
        }
    }

    @Test
    void connectFailsWhenSocketDoesNotExist() {
        Path missing = Path.of(System.getProperty("java.io.tmpdir"), "qmp-test-missing-" + System.nanoTime() + ".sock");
        UnixSocketQmpTransport transport = new UnixSocketQmpTransport(missing);

        assertThatThrownBy(() -> transport.connect(Duration.ofSeconds(1))).isInstanceOf(IOException.class);
    }
}
