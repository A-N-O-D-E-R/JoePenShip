package io.virtualization.sdk.incus.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal in-process stand-in for the Incus REST API, built on the JDK's built-in {@link
 * HttpServer} over plain HTTP (the mutual-TLS transport is Incus-side, not part of what this fake
 * exercises — {@code IncusApiClientTest} talks to it via {@code IncusApiClient}'s package-private
 * plain-{@code HttpClient} constructor). Supports exactly what {@code IncusProvider} and {@code
 * IncusApiClient} need: {@code /instances}, VM lifecycle state changes and operation wait polling.
 */
public final class FakeIncusServer implements AutoCloseable {

    private static final Pattern INSTANCE_PATH = Pattern.compile("/1.0/instances/([^/?]+)");
    private static final Pattern STATE_PATH = Pattern.compile("/1.0/instances/([^/?]+)/state");
    private static final Pattern OPERATION_WAIT_PATH = Pattern.compile("/1.0/operations/([^/?]+)/wait");
    private static final Pattern IMAGE_ALIAS_PATH = Pattern.compile("/1.0/images/aliases/(.+)");
    private static final Pattern IMAGE_EXPORT_PATH = Pattern.compile("/1.0/images/([^/?]+)/export");
    private static final Pattern IMAGE_PATH = Pattern.compile("/1.0/images/([^/?]+)");

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpServer server;
    private final Map<String, InstanceState> instances = new ConcurrentHashMap<>();
    private final Map<String, OperationState> operations = new ConcurrentHashMap<>();
    private final Map<String, ImageState> images = new ConcurrentHashMap<>();
    private final AtomicLong operationSequence = new AtomicLong();
    private volatile int pollsBeforeCompletion = 0;
    private volatile boolean nextOperationFails = false;
    private volatile String pullProgressText;
    private volatile PullRequest lastPullRequest;
    private volatile int lastImportSize = -1;
    private volatile Map<String, Object> lastInstanceCreateRequest;

    public FakeIncusServer() {
        try {
            this.server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
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

    public void addInstance(String name, String type, String status, Map<String, String> config) {
        addInstance(name, type, status, "", config);
    }

    public void addInstance(String name, String type, String status, String location, Map<String, String> config) {
        instances.put(name, new InstanceState(type, status, location, config));
    }

    public void addImage(
            String fingerprint, String type, String architecture, Map<String, String> properties, long size,
            String createdAt, List<String> aliasNames) {
        images.put(fingerprint, new ImageState(type, architecture, properties, size, createdAt, aliasNames));
    }

    /** Sets the {@code download_progress} text reported on each poll of the next pull operation while it runs. */
    public void pullProgress(String text) {
        this.pullProgressText = text;
    }

    public Optional<PullRequest> lastPullRequest() {
        return Optional.ofNullable(lastPullRequest);
    }

    /** Byte length of the last raw image upload received by {@code POST /1.0/images}, or {@code -1} if none yet. */
    public int lastImportSize() {
        return lastImportSize;
    }

    public record PullRequest(String alias, String server, String protocol) {}

    /** The full JSON body of the last {@code POST /1.0/instances} request, for assertions. */
    public Optional<Map<String, Object>> lastInstanceCreateRequest() {
        return Optional.ofNullable(lastInstanceCreateRequest);
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            route(exchange);
        } finally {
            exchange.close();
        }
    }

    private void route(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if ("GET".equals(method) && path.equals("/1.0/instances")) {
            List<Map<String, Object>> body = new ArrayList<>();
            instances.forEach((name, instance) -> body.add(toInstanceJson(name, instance)));
            sendEnvelope(exchange, 200, "sync", body);
            return;
        }

        Matcher state = STATE_PATH.matcher(path);
        if ("PUT".equals(method) && state.matches()) {
            handleStateChange(exchange, state.group(1));
            return;
        }

        Matcher instance = INSTANCE_PATH.matcher(path);
        if ("GET".equals(method) && instance.matches()) {
            InstanceState found = instances.get(instance.group(1));
            if (found == null) {
                sendEnvelope(exchange, 404, "error", null);
                return;
            }
            sendEnvelope(exchange, 200, "sync", toInstanceJson(instance.group(1), found));
            return;
        }
        if ("DELETE".equals(method) && instance.matches()) {
            if (!instances.containsKey(instance.group(1))) {
                sendEnvelope(exchange, 404, "error", null);
                return;
            }
            respondWithOperation(exchange);
            return;
        }

        Matcher wait = OPERATION_WAIT_PATH.matcher(path);
        if ("GET".equals(method) && wait.matches()) {
            handleOperationWait(exchange, wait.group(1));
            return;
        }

        if ("POST".equals(method) && path.equals("/1.0/instances")) {
            handleInstanceCreate(exchange);
            return;
        }

        if ("GET".equals(method) && path.equals("/1.0/images")) {
            List<Map<String, Object>> body = new ArrayList<>();
            images.forEach((fingerprint, image) -> body.add(toImageJson(fingerprint, image)));
            sendEnvelope(exchange, 200, "sync", body);
            return;
        }

        if ("POST".equals(method) && path.equals("/1.0/images")) {
            handleImageCreate(exchange);
            return;
        }

        Matcher alias = IMAGE_ALIAS_PATH.matcher(path);
        if ("GET".equals(method) && alias.matches()) {
            handleAliasLookup(exchange, alias.group(1));
            return;
        }

        Matcher export = IMAGE_EXPORT_PATH.matcher(path);
        if ("GET".equals(method) && export.matches()) {
            handleImageExport(exchange, export.group(1));
            return;
        }

        Matcher image = IMAGE_PATH.matcher(path);
        if ("GET".equals(method) && image.matches()) {
            ImageState found = images.get(image.group(1));
            if (found == null) {
                sendEnvelope(exchange, 404, "error", null);
                return;
            }
            sendEnvelope(exchange, 200, "sync", toImageJson(image.group(1), found));
            return;
        }

        sendEnvelope(exchange, 404, "error", null);
    }

    @SuppressWarnings("unchecked")
    private void handleInstanceCreate(HttpExchange exchange) throws IOException {
        byte[] body = exchange.getRequestBody().readAllBytes();
        Map<String, Object> parsed = mapper.readValue(body, Map.class);
        lastInstanceCreateRequest = parsed;

        String name = (String) parsed.get("name");
        String type = (String) parsed.get("type");
        Map<String, String> config = (Map<String, String>) parsed.getOrDefault("config", Map.of());
        instances.put(name, new InstanceState(type, "Stopped", "", config));

        sendOperationCreatedEnvelope(exchange, createOperation(null, null));
    }

    @SuppressWarnings("unchecked")
    private void handleImageCreate(HttpExchange exchange) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        byte[] body = exchange.getRequestBody().readAllBytes();

        if (contentType != null && contentType.startsWith("application/json")) {
            Map<String, Object> parsed = mapper.readValue(body, Map.class);
            Map<String, Object> source = (Map<String, Object>) parsed.get("source");
            String alias = (String) source.get("alias");
            String server = (String) source.get("server");
            String protocol = (String) source.get("protocol");
            lastPullRequest = new PullRequest(alias, server, protocol);

            String fingerprint = "pulled-" + alias.replace('/', '-');
            images.put(fingerprint, new ImageState("container", "x86_64", Map.of(), 42_000L, "2024-01-01T00:00:00Z", List.of(alias)));
            String operationId = createOperation(fingerprint, pullProgressText);
            pullProgressText = null;
            sendOperationCreatedEnvelope(exchange, operationId);
            return;
        }

        lastImportSize = body.length;
        String fingerprint = "imported-" + operationSequence.incrementAndGet();
        images.put(fingerprint, new ImageState("container", "x86_64", Map.of(), body.length, "2024-01-01T00:00:00Z", List.of()));
        String operationId = createOperation(fingerprint, null);
        sendOperationCreatedEnvelope(exchange, operationId);
    }

    private void handleImageExport(HttpExchange exchange, String fingerprint) throws IOException {
        if (!images.containsKey(fingerprint)) {
            sendEnvelope(exchange, 404, "error", null);
            return;
        }
        byte[] content = ("fake-export-bytes-for-" + fingerprint).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/octet-stream");
        exchange.sendResponseHeaders(200, content.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(content);
        }
    }

    private void handleAliasLookup(HttpExchange exchange, String aliasName) throws IOException {
        for (Map.Entry<String, ImageState> entry : images.entrySet()) {
            if (entry.getValue().aliasNames.contains(aliasName)) {
                Map<String, Object> aliasJson = new LinkedHashMap<>();
                aliasJson.put("name", aliasName);
                aliasJson.put("target", entry.getKey());
                aliasJson.put("description", "");
                aliasJson.put("type", entry.getValue().type);
                sendEnvelope(exchange, 200, "sync", aliasJson);
                return;
            }
        }
        sendEnvelope(exchange, 404, "error", null);
    }

    private static Map<String, Object> toImageJson(String fingerprint, ImageState image) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("fingerprint", fingerprint);
        json.put("filename", fingerprint + ".tar.gz");
        json.put("size", image.size);
        json.put("architecture", image.architecture);
        json.put("cached", true);
        json.put("public", false);
        json.put("type", image.type);
        json.put("created_at", image.createdAt);
        json.put("uploaded_at", image.createdAt);
        json.put("properties", image.properties);
        List<Map<String, String>> aliasesJson = new ArrayList<>();
        for (String aliasName : image.aliasNames) {
            aliasesJson.add(Map.of("name", aliasName, "description", ""));
        }
        json.put("aliases", aliasesJson);
        return json;
    }

    private void handleStateChange(HttpExchange exchange, String name) throws IOException {
        if (!instances.containsKey(name)) {
            sendEnvelope(exchange, 404, "error", null);
            return;
        }
        respondWithOperation(exchange);
    }

    private void respondWithOperation(HttpExchange exchange) throws IOException {
        sendOperationCreatedEnvelope(exchange, createOperation(null, null));
    }

    private String createOperation(String resultFingerprint, String progressText) {
        String operationId = "op-" + operationSequence.incrementAndGet();
        operations.put(operationId, new OperationState(pollsBeforeCompletion, !nextOperationFails, resultFingerprint, progressText));
        nextOperationFails = false;
        return operationId;
    }

    private void sendOperationCreatedEnvelope(HttpExchange exchange, String operationId) throws IOException {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("type", "async");
        envelope.put("status", "Operation created");
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

        Map<String, Object> resource = new LinkedHashMap<>();
        if (operation.pollsRemaining.getAndDecrement() > 0) {
            resource.put("status", "Running");
            resource.put("err", "");
            Map<String, Object> nested = new LinkedHashMap<>();
            if (operation.progressText != null) {
                nested.put("download_progress", operation.progressText);
            }
            resource.put("metadata", nested);
            sendEnvelope(exchange, 200, "sync", resource);
            return;
        }

        resource.put("status", operation.willSucceed ? "Success" : "Failure");
        resource.put("err", operation.willSucceed ? "" : "operation failed");
        Map<String, Object> nested = new LinkedHashMap<>();
        if (operation.willSucceed && operation.resultFingerprint != null) {
            nested.put("fingerprint", operation.resultFingerprint);
        }
        resource.put("metadata", nested);
        sendEnvelope(exchange, 200, "sync", resource);
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

    private record ImageState(
            String type, String architecture, Map<String, String> properties, long size, String createdAt,
            List<String> aliasNames) {}

    private static final class OperationState {
        final AtomicInteger pollsRemaining;
        final boolean willSucceed;
        final String resultFingerprint;
        final String progressText;

        OperationState(int pollsRemaining, boolean willSucceed, String resultFingerprint, String progressText) {
            this.pollsRemaining = new AtomicInteger(pollsRemaining);
            this.willSucceed = willSucceed;
            this.resultFingerprint = resultFingerprint;
            this.progressText = progressText;
        }
    }
}
