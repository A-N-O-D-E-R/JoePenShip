package io.sablier.core;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Objects;

/** Readiness via an HTTP GET — any 2xx response means ready. Uses only {@code java.net.http}, no extra dependency. */
public final class HttpReadinessChecker implements ReadinessChecker, AutoCloseable {

    private final HttpClient httpClient;
    private final URI url;
    private final Duration requestTimeout;

    public HttpReadinessChecker(URI url, Duration requestTimeout) {
        this.url = Objects.requireNonNull(url, "url must not be null");
        this.requestTimeout = Objects.requireNonNull(requestTimeout, "requestTimeout must not be null");
        if (requestTimeout.isNegative() || requestTimeout.isZero()) {
            throw new IllegalArgumentException("requestTimeout must be positive");
        }
        this.httpClient = HttpClient.newBuilder().connectTimeout(requestTimeout).build();
    }

    @Override
    public ReadinessStatus check(Workload workload) {
        HttpRequest request = HttpRequest.newBuilder(url).timeout(requestTimeout).GET().build();
        try {
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            int status = response.statusCode();
            return status >= 200 && status < 300 ? ReadinessStatus.ready() : ReadinessStatus.pending("HTTP " + status + " from " + url);
        } catch (IOException e) {
            return ReadinessStatus.pending("HTTP request to " + url + " failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ReadinessStatus.pending("Interrupted while checking " + url);
        }
    }

    @Override
    public void close() {
        httpClient.close();
    }
}
