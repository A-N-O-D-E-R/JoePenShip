package io.virtualization.sdk.caddy.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.virtualization.sdk.caddy.CaddyClientConfig;
import io.virtualization.sdk.core.exception.ConnectionException;
import io.virtualization.sdk.core.exception.OperationException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

/**
 * Low-level HTTP + JSON client for Caddy's admin API. Deliberately minimal — two calls only, no
 * background-operation polling, no streaming, no mutual TLS (Caddy's admin API defaults to
 * plaintext loopback-only): dramatically smaller than {@code IncusApiClient}. Domain mapping
 * happens in {@link io.virtualization.sdk.caddy.CaddyLoadBalancerProvider}.
 */
public final class CaddyApiClient implements AutoCloseable {

    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final URI endpoint;
    private final Duration requestTimeout;

    public CaddyApiClient(CaddyClientConfig config) {
        Objects.requireNonNull(config, "config must not be null");
        this.endpoint = config.endpoint();
        this.requestTimeout = config.requestTimeout();
        this.httpClient = HttpClient.newBuilder().connectTimeout(config.connectTimeout()).build();
    }

    /** Visible for tests, to inject a client talking to a fake server. */
    public CaddyApiClient(HttpClient httpClient, URI endpoint, Duration requestTimeout) {
        this.httpClient = httpClient;
        this.endpoint = endpoint;
        this.requestTimeout = requestTimeout;
    }

    /** {@code GET /config/} — {@code null} if Caddy has no config loaded yet. */
    public JsonNode getConfig() {
        HttpRequest request = HttpRequest.newBuilder(endpoint.resolve("/config/")).timeout(requestTimeout).GET().build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() == 404) {
            return null;
        }
        checkStatus(request, response.statusCode());
        String body = response.body();
        if (body == null || body.isBlank() || "null".equals(body.trim())) {
            return null;
        }
        return parse(body);
    }

    /** {@code POST /load} — replaces the whole config document, atomically, after Caddy validates it. */
    public void loadConfig(JsonNode config) {
        String json;
        try {
            json = mapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unreachable: config always serializes", e);
        }
        HttpRequest request = HttpRequest.newBuilder(endpoint.resolve("/load"))
                .header("Content-Type", "application/json")
                .timeout(requestTimeout)
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = send(request);
        checkStatus(request, response.statusCode());
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new ConnectionException("Failed to reach Caddy admin API at " + endpoint, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConnectionException("Interrupted while calling Caddy admin API at " + endpoint, e);
        }
    }

    private void checkStatus(HttpRequest request, int code) {
        if (code >= 200 && code < 300) {
            return;
        }
        throw new OperationException("Caddy admin API returned HTTP " + code + " for " + request.method() + " " + request.uri().getPath());
    }

    private JsonNode parse(String body) {
        try {
            return mapper.readTree(body);
        } catch (IOException e) {
            throw new ConnectionException("Failed to parse Caddy admin API response as JSON", e);
        }
    }

    @Override
    public void close() {
        httpClient.close();
    }
}
