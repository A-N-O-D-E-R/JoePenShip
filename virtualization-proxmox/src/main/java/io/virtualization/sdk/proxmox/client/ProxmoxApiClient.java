package io.virtualization.sdk.proxmox.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.virtualization.sdk.core.exception.AuthenticationException;
import io.virtualization.sdk.core.exception.AuthorizationException;
import io.virtualization.sdk.core.exception.ConnectionException;
import io.virtualization.sdk.core.exception.OperationException;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.proxmox.ProxmoxClientConfig;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Objects;

/**
 * Low-level HTTP + JSON client for the Proxmox VE REST API ({@code /api2/json/...}).
 *
 * <p>Translates transport and HTTP-status failures into the SDK's provider-neutral exception
 * hierarchy; never logs the API token. Callers deal in DTOs and Proxmox's {@code "data"} envelope
 * only — the {@link io.virtualization.sdk.core} domain mapping happens one layer up, in {@link
 * io.virtualization.sdk.proxmox.ProxmoxProvider}.
 */
public final class ProxmoxApiClient implements AutoCloseable {

    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final URI endpoint;
    private final String authorizationHeader;
    private final java.time.Duration requestTimeout;

    public ProxmoxApiClient(ProxmoxClientConfig config) {
        Objects.requireNonNull(config, "config must not be null");
        this.endpoint = config.endpoint();
        this.authorizationHeader = config.credentials().toAuthorizationHeaderValue();
        this.requestTimeout = config.requestTimeout();

        HttpClient.Builder builder = HttpClient.newBuilder().connectTimeout(config.connectTimeout());
        if (!config.verifySsl()) {
            builder.sslContext(trustAllSslContext());
        }
        this.httpClient = builder.build();
    }

    public <T> List<T> getList(String path, Class<T> elementType) {
        JsonNode data = requestForData(HttpRequest.newBuilder(resolve(path)).GET());
        return mapper.convertValue(data, mapper.getTypeFactory().constructCollectionType(List.class, elementType));
    }

    public <T> T getSingle(String path, Class<T> type) {
        JsonNode data = requestForData(HttpRequest.newBuilder(resolve(path)).GET());
        return mapper.convertValue(data, type);
    }

    /** POSTs with no body and returns the Proxmox task UPID from the response's {@code "data"} field. */
    public String postForTaskId(String path) {
        JsonNode data = requestForData(HttpRequest.newBuilder(resolve(path)).POST(HttpRequest.BodyPublishers.noBody()));
        return data.asText();
    }

    /** DELETEs and returns the Proxmox task UPID from the response's {@code "data"} field. */
    public String deleteForTaskId(String path) {
        JsonNode data = requestForData(HttpRequest.newBuilder(resolve(path)).DELETE());
        return data.asText();
    }

    private JsonNode requestForData(HttpRequest.Builder requestBuilder) {
        HttpRequest request = requestBuilder
                .header("Authorization", authorizationHeader)
                .header("Accept", "application/json")
                .timeout(requestTimeout)
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new ConnectionException("Failed to reach Proxmox API at " + endpoint, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConnectionException("Interrupted while calling Proxmox API at " + endpoint, e);
        }

        checkStatus(request, response);
        return extractData(response.body());
    }

    private void checkStatus(HttpRequest request, HttpResponse<String> response) {
        int code = response.statusCode();
        if (code >= 200 && code < 300) {
            return;
        }
        String target = request.method() + " " + request.uri().getPath();
        switch (code) {
            case 401 -> throw new AuthenticationException("Proxmox rejected the API token for " + target);
            case 403 -> throw new AuthorizationException("Proxmox denied access for " + target);
            case 404 -> throw new ResourceNotFoundException("Proxmox resource not found: " + target);
            default -> throw new OperationException("Proxmox API returned HTTP " + code + " for " + target);
        }
    }

    private JsonNode extractData(String body) {
        JsonNode root;
        try {
            root = mapper.readTree(body);
        } catch (IOException e) {
            throw new ConnectionException("Failed to parse Proxmox API response as JSON", e);
        }
        JsonNode data = root.get("data");
        if (data == null) {
            throw new ConnectionException("Proxmox API response is missing the expected 'data' field");
        }
        return data;
    }

    private URI resolve(String path) {
        return endpoint.resolve("/api2/json" + path);
    }

    private static SSLContext trustAllSslContext() {
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[] {new PermissiveTrustManager()}, new SecureRandom());
            return context;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to build permissive SSL context", e);
        }
    }

    /** Accepts any certificate chain. Only used when the caller explicitly sets {@code verifySsl=false}. */
    private static final class PermissiveTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {}

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {}

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    @Override
    public void close() {
        httpClient.close();
    }
}
