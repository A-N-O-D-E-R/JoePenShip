package io.virtualization.sdk.qemu.qmp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.virtualization.sdk.core.exception.ConnectionException;
import io.virtualization.sdk.core.exception.OperationException;
import io.virtualization.sdk.core.exception.VirtualizationException;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * A reusable client for QEMU's QMP control protocol: performs the greeting / {@code
 * qmp_capabilities} handshake, correlates command requests with responses by id, and dispatches
 * asynchronous events to registered listeners — independent of whether the underlying {@link
 * QmpTransport} is a Unix domain socket or TCP.
 *
 * <p>Thread-safety: concurrent {@link #execute} calls from multiple threads are safe. Calling
 * {@link #reconnect()} concurrently with in-flight {@link #execute} calls is not guaranteed to be
 * safe — this client is meant to be driven by one owner (a single {@code QemuProvider}) at a time.
 */
public final class QmpClient implements AutoCloseable {

    private final ObjectMapper mapper = new ObjectMapper();
    private final QmpEndpoint endpoint;
    private final Duration connectTimeout;
    private final Duration commandTimeout;
    private final List<Consumer<QmpEvent>> listeners = new CopyOnWriteArrayList<>();
    private final AtomicLong idSequence = new AtomicLong();

    // Both fields are replaced together on connect/reconnect. The reader thread and execute()
    // capture them once per call/generation rather than re-reading these fields mid-flight, so a
    // stale reader thread from a connection that reconnect() just closed can never fail pending
    // futures that belong to the new generation.
    private volatile QmpTransport transport;
    private volatile Map<String, CompletableFuture<JsonNode>> pending = new ConcurrentHashMap<>();
    private volatile boolean closed;

    private QmpClient(QmpEndpoint endpoint, Duration connectTimeout, Duration commandTimeout) {
        this.endpoint = endpoint;
        this.connectTimeout = connectTimeout;
        this.commandTimeout = commandTimeout;
    }

    public static QmpClient connect(QmpEndpoint endpoint, Duration connectTimeout, Duration commandTimeout) {
        Objects.requireNonNull(endpoint, "endpoint must not be null");
        Objects.requireNonNull(connectTimeout, "connectTimeout must not be null");
        Objects.requireNonNull(commandTimeout, "commandTimeout must not be null");
        QmpClient client = new QmpClient(endpoint, connectTimeout, commandTimeout);
        client.open();
        return client;
    }

    /** Registers a listener invoked for every asynchronous QMP event, including after a {@link #reconnect()}. */
    public void addEventListener(Consumer<QmpEvent> listener) {
        listeners.add(Objects.requireNonNull(listener, "listener must not be null"));
    }

    public JsonNode execute(String command) {
        return execute(command, null);
    }

    public JsonNode execute(String command, Map<String, Object> arguments) {
        if (closed) {
            throw new ConnectionException("QMP client is closed");
        }
        QmpTransport activeTransport = this.transport;
        Map<String, CompletableFuture<JsonNode>> activePending = this.pending;
        String id = "cmd-" + idSequence.incrementAndGet();
        CompletableFuture<JsonNode> future = new CompletableFuture<>();
        activePending.put(id, future);
        try {
            activeTransport.writeLine(mapper.writeValueAsString(buildRequest(command, id, arguments)));
        } catch (IOException e) {
            activePending.remove(id);
            throw new ConnectionException("Failed to send QMP command '" + command + "'", e);
        }
        return awaitResponse(command, id, activePending, future);
    }

    /** Closes the current connection and re-establishes it (new transport, fresh handshake). */
    public void reconnect() {
        if (closed) {
            throw new ConnectionException("QMP client is closed");
        }
        closeTransportQuietly();
        failAllPending(this.pending, new ConnectionException("QMP client is reconnecting"));
        open();
    }

    @Override
    public void close() {
        closed = true;
        closeTransportQuietly();
        failAllPending(this.pending, new ConnectionException("QMP client closed"));
    }

    private void open() {
        QmpTransport t = createTransport(endpoint);
        try {
            t.connect(connectTimeout);
            String greeting = t.readLine();
            if (greeting == null || !isGreeting(greeting)) {
                throw new IOException("Did not receive a valid QMP greeting");
            }
        } catch (IOException e) {
            throw new ConnectionException("Failed to connect to QMP endpoint " + endpoint, e);
        }
        Map<String, CompletableFuture<JsonNode>> newPending = new ConcurrentHashMap<>();
        this.transport = t;
        this.pending = newPending;
        startReaderThread(t, newPending);
        execute("qmp_capabilities");
    }

    private boolean isGreeting(String line) {
        try {
            return mapper.readTree(line).has("QMP");
        } catch (IOException e) {
            return false;
        }
    }

    private static QmpTransport createTransport(QmpEndpoint endpoint) {
        return switch (endpoint) {
            case UnixSocketEndpoint unix -> new UnixSocketQmpTransport(unix.path());
            case TcpEndpoint tcp -> new TcpQmpTransport(tcp.host(), tcp.port());
        };
    }

    private ObjectNode buildRequest(String command, String id, Map<String, Object> arguments) {
        ObjectNode request = mapper.createObjectNode();
        request.put("execute", command);
        request.put("id", id);
        if (arguments != null && !arguments.isEmpty()) {
            request.set("arguments", mapper.valueToTree(arguments));
        }
        return request;
    }

    private JsonNode awaitResponse(
            String command, String id, Map<String, CompletableFuture<JsonNode>> activePending, CompletableFuture<JsonNode> future) {
        try {
            return future.get(commandTimeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            activePending.remove(id);
            throw new OperationException("QMP command '" + command + "' timed out after " + commandTimeout);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof VirtualizationException virtualizationException) {
                throw virtualizationException;
            }
            throw new OperationException("QMP command '" + command + "' failed", cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            activePending.remove(id);
            throw new ConnectionException("Interrupted while awaiting QMP command '" + command + "'", e);
        }
    }

    private void startReaderThread(QmpTransport activeTransport, Map<String, CompletableFuture<JsonNode>> activePending) {
        Thread.ofVirtual().name("qmp-reader").start(() -> readLoop(activeTransport, activePending));
    }

    private void readLoop(QmpTransport activeTransport, Map<String, CompletableFuture<JsonNode>> activePending) {
        while (true) {
            String line;
            try {
                line = activeTransport.readLine();
            } catch (IOException e) {
                failAllPending(activePending, new ConnectionException("QMP connection lost", e));
                return;
            }
            if (line == null) {
                failAllPending(activePending, new ConnectionException("QMP connection closed by peer"));
                return;
            }
            if (line.isBlank()) {
                continue;
            }
            dispatchLine(line, activePending);
        }
    }

    private void dispatchLine(String line, Map<String, CompletableFuture<JsonNode>> activePending) {
        JsonNode root;
        try {
            root = mapper.readTree(line);
        } catch (IOException e) {
            return; // malformed line from the peer, ignore rather than kill the reader loop
        }
        if (root.has("event")) {
            dispatchEvent(root);
        } else if (root.has("id")) {
            dispatchResponse(root, activePending);
        }
    }

    private void dispatchEvent(JsonNode root) {
        QmpEvent event = new QmpEvent(root.path("event").asText(), root.path("data"));
        for (Consumer<QmpEvent> listener : listeners) {
            try {
                listener.accept(event);
            } catch (RuntimeException ignored) {
                // a misbehaving listener must not take down the reader loop
            }
        }
    }

    private void dispatchResponse(JsonNode root, Map<String, CompletableFuture<JsonNode>> activePending) {
        CompletableFuture<JsonNode> future = activePending.remove(root.path("id").asText());
        if (future == null) {
            return;
        }
        if (root.has("error")) {
            JsonNode error = root.get("error");
            future.completeExceptionally(new OperationException(
                    "QMP command failed: " + error.path("class").asText() + ": " + error.path("desc").asText()));
        } else {
            future.complete(root.get("return"));
        }
    }

    private void failAllPending(Map<String, CompletableFuture<JsonNode>> activePending, VirtualizationException cause) {
        activePending.forEach((id, future) -> future.completeExceptionally(cause));
        activePending.clear();
    }

    private void closeTransportQuietly() {
        QmpTransport t = this.transport;
        if (t != null) {
            try {
                t.close();
            } catch (IOException ignored) {
                // best-effort
            }
        }
    }
}
