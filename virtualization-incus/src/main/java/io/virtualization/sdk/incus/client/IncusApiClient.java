package io.virtualization.sdk.incus.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.virtualization.sdk.core.exception.AuthenticationException;
import io.virtualization.sdk.core.exception.AuthorizationException;
import io.virtualization.sdk.core.exception.ConfigurationException;
import io.virtualization.sdk.core.exception.ConnectionException;
import io.virtualization.sdk.core.exception.OperationException;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.incus.IncusClientConfig;
import io.virtualization.sdk.incus.IncusTlsCredentials;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

/**
 * Low-level HTTP + JSON client for the Incus REST API ({@code /1.0/...}).
 *
 * <p>Authentication is entirely at the TLS layer (mutual TLS with a trusted client certificate) —
 * there is no per-request credential header to attach or redact. Translates transport and
 * HTTP-status failures into the SDK's provider-neutral exception hierarchy. Callers deal in DTOs
 * and Incus's {@code "metadata"} envelope only — domain mapping happens in {@link
 * io.virtualization.sdk.incus.IncusProvider}.
 */
public final class IncusApiClient implements AutoCloseable {

    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();
    private final URI endpoint;
    private final Duration requestTimeout;
    private final String project;

    public IncusApiClient(IncusClientConfig config) {
        Objects.requireNonNull(config, "config must not be null");
        this.endpoint = config.endpoint();
        this.requestTimeout = config.requestTimeout();
        this.project = config.project();

        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(config.connectTimeout())
                .sslContext(buildSslContext(config.credentials(), config.verifySsl()));
        this.httpClient = builder.build();
    }

    /**
     * Advanced escape hatch: talk through a caller-supplied {@link HttpClient}, bypassing
     * PEM-based {@link SSLContext} construction entirely. Intended for tests (a plain-HTTP fake
     * server) or callers who need to preconfigure the {@code HttpClient} themselves. Scoped to the
     * {@code "default"} project.
     */
    public IncusApiClient(HttpClient httpClient, URI endpoint, Duration requestTimeout) {
        this(httpClient, endpoint, requestTimeout, "default");
    }

    /** Same as {@link #IncusApiClient(HttpClient, URI, Duration)}, scoped to an explicit project. */
    public IncusApiClient(HttpClient httpClient, URI endpoint, Duration requestTimeout, String project) {
        this.httpClient = httpClient;
        this.endpoint = endpoint;
        this.requestTimeout = requestTimeout;
        this.project = project;
    }

    public <T> List<T> getList(String path, Class<T> elementType) {
        JsonNode metadata = requestForMetadata(HttpRequest.newBuilder(resolve(path)).GET());
        return mapper.convertValue(metadata, mapper.getTypeFactory().constructCollectionType(List.class, elementType));
    }

    public <T> T getSingle(String path, Class<T> type) {
        JsonNode metadata = requestForMetadata(HttpRequest.newBuilder(resolve(path)).GET());
        return mapper.convertValue(metadata, type);
    }

    /**
     * PUTs a JSON body and returns the background operation's id, or {@code null} if Incus
     * completed the request synchronously (no operation to await).
     */
    public String putForOperationId(String path, String jsonBody) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(resolve(path))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));
        return requestForOperationId(builder);
    }

    /** DELETEs and returns the background operation's id, or {@code null} if handled synchronously. */
    public String deleteForOperationId(String path) {
        return requestForOperationId(HttpRequest.newBuilder(resolve(path)).DELETE());
    }

    /** POSTs a JSON body and returns the background operation's id, or {@code null} if handled synchronously. */
    public String postForOperationId(String path, String jsonBody) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(resolve(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8));
        return requestForOperationId(builder);
    }

    /**
     * POSTs a streamed binary body (e.g. an image tarball) and returns the background operation's
     * id, or {@code null} if handled synchronously. The body is streamed rather than buffered.
     */
    public String postStreamForOperationId(String path, InputStream body) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(resolve(path))
                .header("Content-Type", "application/octet-stream")
                .POST(HttpRequest.BodyPublishers.ofInputStream(() -> body));
        return requestForOperationId(builder);
    }

    /**
     * GETs a raw binary response (e.g. an image export) without parsing it as an Incus JSON
     * envelope. The caller must close the returned response's {@link InputStream} body.
     */
    public HttpResponse<InputStream> getStream(String path) {
        HttpRequest request = HttpRequest.newBuilder(resolve(path)).timeout(requestTimeout).GET().build();
        HttpResponse<InputStream> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException e) {
            throw new ConnectionException("Failed to reach Incus API at " + endpoint, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConnectionException("Interrupted while calling Incus API at " + endpoint, e);
        }
        checkStatus(request, response.statusCode());
        return response;
    }

    private String requestForOperationId(HttpRequest.Builder requestBuilder) {
        HttpRequest request = requestBuilder.timeout(requestTimeout).build();
        HttpResponse<String> response = send(request);
        checkStatus(request, response.statusCode());

        JsonNode root = parseRoot(response.body());
        if ("sync".equals(root.path("type").asText())) {
            return null;
        }
        String operation = root.path("operation").asText(null);
        if (operation == null || operation.isBlank()) {
            throw new ConnectionException("Incus response missing 'operation' field for an asynchronous request");
        }
        String[] segments = operation.split("/");
        return segments[segments.length - 1];
    }

    private JsonNode requestForMetadata(HttpRequest.Builder requestBuilder) {
        HttpRequest request = requestBuilder.timeout(requestTimeout).build();
        HttpResponse<String> response = send(request);
        checkStatus(request, response.statusCode());

        JsonNode root = parseRoot(response.body());
        JsonNode metadata = root.get("metadata");
        if (metadata == null) {
            throw new ConnectionException("Incus API response is missing the expected 'metadata' field");
        }
        return metadata;
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new ConnectionException("Failed to reach Incus API at " + endpoint, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConnectionException("Interrupted while calling Incus API at " + endpoint, e);
        }
    }

    private void checkStatus(HttpRequest request, int code) {
        if (code >= 200 && code < 300) {
            return;
        }
        String target = request.method() + " " + request.uri().getPath();
        switch (code) {
            case 401 -> throw new AuthenticationException("Incus rejected the request for " + target);
            case 403 -> throw new AuthorizationException("Incus denied access for " + target);
            case 404 -> throw new ResourceNotFoundException("Incus resource not found: " + target);
            default -> throw new OperationException("Incus API returned HTTP " + code + " for " + target);
        }
    }

    private JsonNode parseRoot(String body) {
        try {
            return mapper.readTree(body);
        } catch (IOException e) {
            throw new ConnectionException("Failed to parse Incus API response as JSON", e);
        }
    }

    private URI resolve(String path) {
        String withProject = path.contains("?") ? path + "&project=" + encode(project) : path + "?project=" + encode(project);
        return endpoint.resolve("/1.0" + withProject);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static SSLContext buildSslContext(IncusTlsCredentials credentials, boolean verifySsl) {
        try {
            KeyManager[] keyManagers = buildKeyManagers(credentials);
            TrustManager[] trustManagers = buildTrustManagers(credentials, verifySsl);
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(keyManagers, trustManagers, new SecureRandom());
            return context;
        } catch (GeneralSecurityException e) {
            throw new ConfigurationException("Failed to build TLS context from Incus client credentials", e);
        }
    }

    /** {@code null} means "use the JVM's default trust store" — the normal case for a real, CA-signed endpoint. */
    private static TrustManager[] buildTrustManagers(IncusTlsCredentials credentials, boolean verifySsl)
            throws GeneralSecurityException {
        if (!verifySsl) {
            return new TrustManager[] {new PermissiveTrustManager()};
        }
        if (credentials.caCertificatePem().isEmpty()) {
            return null;
        }
        X509Certificate ca = parseCertificate(credentials.caCertificatePem().get());
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        try {
            trustStore.load(null, null);
        } catch (IOException e) {
            throw new IllegalStateException("Unreachable: in-memory keystore load never touches I/O", e);
        }
        trustStore.setCertificateEntry("incus-ca", ca);
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(trustStore);
        return factory.getTrustManagers();
    }

    private static KeyManager[] buildKeyManagers(IncusTlsCredentials credentials) throws GeneralSecurityException {
        X509Certificate certificate = parseCertificate(credentials.clientCertificatePem());
        PrivateKey privateKey = parsePrivateKey(credentials.clientKeyPem());

        char[] password = new char[0];
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try {
            keyStore.load(null, null);
        } catch (IOException e) {
            throw new IllegalStateException("Unreachable: in-memory keystore load never touches I/O", e);
        }
        keyStore.setKeyEntry("incus-client", privateKey, password, new Certificate[] {certificate});

        KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        factory.init(keyStore, password);
        return factory.getKeyManagers();
    }

    private static X509Certificate parseCertificate(String pem) {
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            return (X509Certificate)
                    factory.generateCertificate(new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new ConfigurationException("Failed to parse Incus client certificate PEM", e);
        }
    }

    private static PrivateKey parsePrivateKey(String pem) {
        String base64 = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException(
                    "Failed to parse Incus client key PEM: not valid PKCS#8 (only PKCS#8 'BEGIN PRIVATE KEY' is supported)", e);
        }
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        for (String algorithm : List.of("RSA", "EC")) {
            try {
                return KeyFactory.getInstance(algorithm).generatePrivate(spec);
            } catch (GeneralSecurityException ignored) {
                // try the next algorithm
            }
        }
        throw new ConfigurationException("Failed to parse Incus client key PEM as an RSA or EC PKCS#8 key");
    }

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
