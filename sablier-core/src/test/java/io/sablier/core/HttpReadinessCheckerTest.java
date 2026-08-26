package io.sablier.core;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HttpReadinessCheckerTest {

    private static final Workload WORKLOAD =
            new Workload("w-1", "jellyfin", WorkloadType.CONTAINER, WorkloadState.RUNNING, "media", "default", Optional.empty());

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    private HttpServer startServer(int status) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        httpServer.createContext("/health", exchange -> {
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        httpServer.start();
        return httpServer;
    }

    @Test
    void readyOn2xxResponse() throws IOException {
        server = startServer(200);
        try (HttpReadinessChecker checker =
                new HttpReadinessChecker(URI.create("http://localhost:" + server.getAddress().getPort() + "/health"), Duration.ofSeconds(2))) {
            assertThat(checker.check(WORKLOAD).state()).isEqualTo(ReadinessState.READY);
        }
    }

    @Test
    void pendingOnNon2xxResponse() throws IOException {
        server = startServer(503);
        try (HttpReadinessChecker checker =
                new HttpReadinessChecker(URI.create("http://localhost:" + server.getAddress().getPort() + "/health"), Duration.ofSeconds(2))) {
            assertThat(checker.check(WORKLOAD).state()).isEqualTo(ReadinessState.PENDING);
        }
    }

    @Test
    void pendingWhenUnreachable() {
        try (HttpReadinessChecker checker = new HttpReadinessChecker(URI.create("http://localhost:1/health"), Duration.ofMillis(500))) {
            assertThat(checker.check(WORKLOAD).state()).isEqualTo(ReadinessState.PENDING);
        }
    }
}
