package io.virtualization.sdk.caddy.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;

/**
 * Minimal in-process stand-in for Caddy's admin API, built on the JDK's built-in {@link
 * HttpServer} — exact {@code FakeIncusServer} construction style, far simpler content: {@code GET
 * /config/} returns whatever was last {@code POST}ed to {@code /load} (or an empty object if
 * nothing has been loaded yet), {@code POST /load} just stores the body and returns 200. No
 * validation, no operations — Caddy's admin API for these two calls is genuinely this simple.
 */
public final class FakeCaddyServer implements AutoCloseable {

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpServer server;
    private volatile JsonNode config;

    public FakeCaddyServer() {
        try {
            this.server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to bind fake Caddy server", e);
        }
        server.createContext("/", this::handle);
    }

    public FakeCaddyServer start() {
        server.start();
        return this;
    }

    public URI uri() {
        return URI.create("http://localhost:" + server.getAddress().getPort());
    }

    /** The full config body of the last {@code POST /load} request, for assertions. */
    public JsonNode lastLoadedConfig() {
        return config;
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

        if ("GET".equals(method) && "/config/".equals(path)) {
            sendJson(exchange, 200, config != null ? config : mapper.createObjectNode());
            return;
        }

        if ("POST".equals(method) && "/load".equals(path)) {
            byte[] body = exchange.getRequestBody().readAllBytes();
            config = mapper.readTree(body);
            exchange.sendResponseHeaders(200, -1);
            return;
        }

        exchange.sendResponseHeaders(404, -1);
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
}
