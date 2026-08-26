package io.virtualization.sdk.qemu.qmp;

import com.fasterxml.jackson.databind.JsonNode;
import io.virtualization.sdk.core.exception.ConnectionException;
import io.virtualization.sdk.core.exception.OperationException;
import io.virtualization.sdk.qemu.qmp.support.FakeQmpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QmpClientTest {

    private FakeQmpServer server;
    private QmpClient client;

    @AfterEach
    void tearDown() {
        if (client != null) {
            client.close();
        }
        if (server != null) {
            server.close();
        }
    }

    private QmpClient connect() {
        server = new FakeQmpServer();
        client = QmpClient.connect(new TcpEndpoint("localhost", server.port()), Duration.ofSeconds(2), Duration.ofSeconds(2));
        return client;
    }

    @Test
    void handshakeSucceedsAndExecuteReturnsResult() {
        connect();

        JsonNode result = client.execute("query-status");

        assertThat(result).isNotNull();
    }

    @Test
    void executeReturnsCannedResponseData() {
        connect();
        server.onCommand("query-status", req -> "{\"return\":{\"status\":\"running\"}}");

        JsonNode result = client.execute("query-status");

        assertThat(result.path("status").asText()).isEqualTo("running");
    }

    @Test
    void errorResponseThrowsOperationException() {
        connect();
        server.respondWithError("stop", "GenericError", "boom");

        assertThatThrownBy(() -> client.execute("stop"))
                .isInstanceOf(OperationException.class)
                .hasMessageContaining("boom");
    }

    @Test
    void commandTimeoutThrowsOperationException() {
        server = new FakeQmpServer();
        server.delayResponses(Duration.ofSeconds(5));

        // qmp_capabilities during the handshake hits the same delay/timeout, so the timeout
        // surfaces from QmpClient.connect() itself here rather than from a later execute() call.
        assertThatThrownBy(() -> QmpClient.connect(
                        new TcpEndpoint("localhost", server.port()), Duration.ofSeconds(2), Duration.ofMillis(100)))
                .isInstanceOf(OperationException.class);
    }

    @Test
    void eventListenerReceivesAsynchronousEvents() {
        connect();
        List<QmpEvent> received = new CopyOnWriteArrayList<>();
        client.addEventListener(received::add);

        server.emitEvent("SHUTDOWN");

        Instant deadline = Instant.now().plusSeconds(5);
        while (received.isEmpty() && Instant.now().isBefore(deadline)) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        assertThat(received).extracting(QmpEvent::name).containsExactly("SHUTDOWN");
    }

    @Test
    void reconnectReestablishesConnection() {
        connect();
        assertThat(client.execute("query-status")).isNotNull();

        client.reconnect();

        assertThat(client.execute("query-status")).isNotNull();
    }

    @Test
    void connectFailsWhenNothingListening() {
        assertThatThrownBy(() -> QmpClient.connect(new TcpEndpoint("localhost", 1), Duration.ofMillis(500), Duration.ofSeconds(1)))
                .isInstanceOf(ConnectionException.class);
    }

    @Test
    void executeAfterCloseThrowsConnectionException() {
        connect();
        client.close();

        assertThatThrownBy(() -> client.execute("query-status")).isInstanceOf(ConnectionException.class);
    }
}
