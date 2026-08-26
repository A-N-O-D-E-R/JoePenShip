package io.sablier.incus.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal in-process stand-in for the Incus REST API, built on the JDK's built-in {@link
 * HttpServer} over plain HTTP (mutual TLS is exercised separately, at the PEM-parsing /
 * SSLContext-building level — {@code IncusClientTest} talks to this fake via {@code
 * IncusClient}'s package-private plain-{@code HttpClient} constructor). Supports project-scoped
 * instance listing/lookup/state, start/stop/restart, and operation wait polling.
 */
public final class FakeIncusServer implements AutoCloseable {

    private static final Pattern INSTANCE_STATE_PATH = Pattern.compile("/1\\.0/instances/([^/?]+)/state");
    private static final Pattern INSTANCE_PATH = Pattern.compile("/1\\.0/instances/([^/?]+)");
    private static final Pattern OPERATION_WAIT_PATH = Pattern.compile("/1\\.0/operations/([^/?]+)/wait");

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpServer server;
    private final Map<String, InstanceState> instances = new ConcurrentHashMap<>();
    private final Map<String, OperationState> operations = new ConcurrentHashMap<>();
    private final AtomicLong operationSequence = new AtomicLong();
    private volatile int pollsBeforeCompletion = 0;
    private volatile boolean nextOperationFails = false;
    private volatile boolean forbidNextRequest = false;

    public FakeIncusServer() {
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to bind fake Incus server", e);
        }
        server.createContext("/1.0/", this::handle);
    }

    public FakeIncusServer start() {
        server.start();
        return this;
    }

    public URI uri() {
        return URI.create("http://localhost:" + server.getAddress().getPort());
    }

    public void pollsBeforeCompletion(int polls) {
        this.pollsBeforeCompletion = polls;
    }

    public void nextOperationFails() {
        this.nextOperationFails = true;
    }

    public void forbidNextRequest() {
        this.forbidNextRequest = true;
    }

    public void addInstance(String project, String name, String type, String status, String location, Map<String, String> config) {
        instances.put(key(project, name), new InstanceState(type, status, location, config));
    }

    private static String key(String project, String name) {
        return project + "/" + name;
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            route(exchange);
        } finally {
            exchange.close();
        }
    }

    private void route(HttpExchange exchange) throws IOException {
        if (forbidNextRequest) {
            forbidNextRequest = false;
            sendEnvelope(exchange, 403, "error", null);
            return;
        }
        String method = exchange.getRequestMethod();
        URI requestUri = exchange.getRequestURI();
        String path = requestUri.getPath();
        String project = queryParam(requestUri, "project");

        if ("GET".equals(method) && path.equals("/1.0/instances")) {
            List<Map<String, Object>> body = new ArrayList<>();
            instances.forEach((k, instance) -> {
                if (k.startsWith(project + "/")) {
                    body.add(toInstanceJson(k.substring(project.length() + 1), instance));
                }
            });
            sendEnvelope(exchange, 200, "sync", body);
            return;
        }

        Matcher state = INSTANCE_STATE_PATH.matcher(path);
        if ("PUT".equals(method) && state.matches()) {
            handleStateChange(exchange, project, state.group(1));
            return;
        }
        if ("GET".equals(method) && state.matches()) {
            InstanceState found = instances.get(key(project, state.group(1)));
            if (found == null) {
                sendEnvelope(exchange, 404, "error", null);
                return;
            }
            sendEnvelope(exchange, 200, "sync", Map.of("status", found.status, "status_code", 100));
            return;
        }

        Matcher instance = INSTANCE_PATH.matcher(path);
        if ("GET".equals(method) && instance.matches()) {
            InstanceState found = instances.get(key(project, instance.group(1)));
            if (found == null) {
                sendEnvelope(exchange, 404, "error", null);
                return;
            }
            sendEnvelope(exchange, 200, "sync", toInstanceJson(instance.group(1), found));
            return;
        }

        Matcher wait = OPERATION_WAIT_PATH.matcher(path);
        if ("GET".equals(method) && wait.matches()) {
            handleOperationWait(exchange, wait.group(1));
            return;
        }

        sendEnvelope(exchange, 404, "error", null);
    }

    private void handleStateChange(HttpExchange exchange, String project, String name) throws IOException {
        if (!instances.containsKey(key(project, name))) {
            sendEnvelope(exchange, 404, "error", null);
            return;
        }
        String operationId = "op-" + operationSequence.incrementAndGet();
        operations.put(operationId, new OperationState(pollsBeforeCompletion, !nextOperationFails));
        nextOperationFails = false;

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("type", "async");
        envelope.put("status_code", 100);
        envelope.put("operation", "/1.0/operations/" + operationId);
        envelope.put("metadata", Map.of("id", operationId, "status", "Running"));
        sendJson(exchange, 202, envelope);
    }

    private void handleOperationWait(HttpExchange exchange, String operationId) throws IOException {
        OperationState operation = operations.get(operationId);
        if (operation == null) {
            sendEnvelope(exchange, 404, "error", null);
            return;
        }
        if (operation.pollsRemaining.getAndDecrement() > 0) {
            sendEnvelope(exchange, 200, "sync", Map.of("status", "Running"));
            return;
        }
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", operation.willSucceed ? "Success" : "Failure");
        status.put("err", operation.willSucceed ? "" : "operation failed");
        sendEnvelope(exchange, 200, "sync", status);
    }

    private static Map<String, Object> toInstanceJson(String name, InstanceState instance) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("name", name);
        json.put("type", instance.type);
        json.put("status", instance.status);
        json.put("location", instance.location);
        json.put("config", instance.config);
        return json;
    }

    private static String queryParam(URI uri, String name) {
        String query = uri.getRawQuery();
        if (query == null) {
            return "";
        }
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq >= 0 && pair.substring(0, eq).equals(name)) {
                return URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            }
        }
        return "";
    }

    private void sendEnvelope(HttpExchange exchange, int status, String type, Object metadata) throws IOException {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("type", type);
        envelope.put("status_code", status);
        envelope.put("metadata", metadata);
        sendJson(exchange, status, envelope);
    }

    private void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] bytes = mapper.writeValueAsBytes(body);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private record InstanceState(String type, String status, String location, Map<String, String> config) {}

    private static final class OperationState {
        final AtomicInteger pollsRemaining;
        final boolean willSucceed;

        OperationState(int pollsRemaining, boolean willSucceed) {
            this.pollsRemaining = new AtomicInteger(pollsRemaining);
            this.willSucceed = willSucceed;
        }
    }
}
