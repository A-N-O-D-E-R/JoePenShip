package io.virtualization.sdk.qemu.qmp.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * Minimal fake QEMU QMP server over TCP, built on plain {@link ServerSocket} (no extra test
 * dependency). Sends the greeting on connect, answers {@code qmp_capabilities} and any command
 * with a canned response, and supports emitting events and simulating disconnects/error
 * responses/slow responses — enough to exercise {@code QmpClient}'s full protocol handling.
 */
public final class FakeQmpServer implements AutoCloseable {

    private static final String GREETING =
            "{\"QMP\":{\"version\":{\"qemu\":{\"major\":8,\"minor\":0,\"micro\":0},\"package\":\"\"},\"capabilities\":[]}}";

    private final ObjectMapper mapper = new ObjectMapper();
    private final ServerSocket serverSocket;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final Map<String, Function<JsonNode, String>> canned = new ConcurrentHashMap<>();
    private volatile PrintWriter activeWriter;
    private volatile Duration responseDelay = Duration.ZERO;

    public FakeQmpServer() {
        try {
            serverSocket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to bind fake QMP server", e);
        }
        executor.submit(this::acceptLoop);
    }

    public int port() {
        return serverSocket.getLocalPort();
    }

    /** Registers a canned response body (without an "id" field) for a given QMP command. */
    public void onCommand(String command, Function<JsonNode, String> handler) {
        canned.put(command, handler);
    }

    public void respondWithError(String command, String errorClass, String description) {
        canned.put(command, req -> "{\"error\":{\"class\":\"" + errorClass + "\",\"desc\":\"" + description + "\"}}");
    }

    public void delayResponses(Duration delay) {
        this.responseDelay = delay;
    }

    public void emitEvent(String eventName) {
        PrintWriter writer = activeWriter;
        if (writer != null) {
            sendLine(writer, "{\"event\":\"" + eventName + "\",\"data\":{}}");
        }
    }

    private void acceptLoop() {
        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                PrintWriter writer =
                        new PrintWriter(new java.io.OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), false);
                activeWriter = writer;
                sendLine(writer, GREETING);
                executor.submit(() -> readLoop(socket, writer));
            } catch (IOException e) {
                return;
            }
        }
    }

    private void readLoop(Socket socket, PrintWriter writer) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                handleLine(writer, line);
            }
        } catch (IOException ignored) {
            // client disconnected
        }
    }

    private void handleLine(PrintWriter writer, String line) {
        JsonNode request;
        try {
            request = mapper.readTree(line);
        } catch (IOException e) {
            return;
        }
        String command = request.path("execute").asText();
        String id = request.has("id") ? request.get("id").asText() : null;

        if (!responseDelay.isZero()) {
            try {
                Thread.sleep(responseDelay.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        Function<JsonNode, String> handler = canned.getOrDefault(command, req -> "{\"return\":{}}");
        sendLine(writer, spliceId(handler.apply(request), id));
    }

    private static String spliceId(String body, String id) {
        if (id == null) {
            return body;
        }
        // body is always a single top-level JSON object literal like {"return":{...}} or {"error":{...}}
        return body.substring(0, body.length() - 1) + ",\"id\":\"" + id + "\"}";
    }

    private static void sendLine(PrintWriter writer, String line) {
        synchronized (writer) {
            writer.write(line);
            writer.write("\n");
            writer.flush();
        }
    }

    @Override
    public void close() {
        try {
            serverSocket.close();
        } catch (IOException ignored) {
            // best-effort
        }
        executor.shutdownNow();
    }
}
