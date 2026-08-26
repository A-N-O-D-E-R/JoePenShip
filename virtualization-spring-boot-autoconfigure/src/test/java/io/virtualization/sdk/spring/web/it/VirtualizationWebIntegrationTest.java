package io.virtualization.sdk.spring.web.it;

import io.virtualization.sdk.core.ProviderRegistry;
import io.virtualization.sdk.core.image.ImageProviderRegistry;
import io.virtualization.sdk.domain.DomainManager;
import io.virtualization.sdk.spring.web.support.FakeImageProvider;
import io.virtualization.sdk.spring.web.support.FakeVirtualizationProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fires real HTTP requests at a real embedded server (Jackson message converters, request
 * mapping, servlet plumbing included) rather than calling controller methods directly — this is
 * what would have caught a JSR-310-module-missing-style bug at this layer.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = VirtualizationWebIntegrationTest.TestApp.class,
        properties = {
                "virtualization.vps.provider=fake",
                "virtualization.domains.enabled=true",
                "virtualization.dns.providers.cloudflare.type=mock",
                "virtualization.dns.providers.cloudflare.zones[0]=example.com",
                "virtualization.certificates.enabled=true",
                "virtualization.certificates.providers.letsencrypt.type=mock",
                "virtualization.certificates.providers.letsencrypt.dns-provider=cloudflare"
        })
class VirtualizationWebIntegrationTest {

    @LocalServerPort
    int port;

    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postJson(String path, String json) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return http.send(request, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void listReturnsImageAsJsonIncludingInstantField() throws Exception {
        HttpResponse<String> response = get("/api/v1/images?provider=fake");

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        assertThat(response.body()).contains("ubuntu/24.04").contains("2024-01-01");
    }

    @Test
    void getUnknownImageReturns404WithErrorBody() throws Exception {
        HttpResponse<String> response = get("/api/v1/images/images:missing?provider=fake");

        assertThat(response.statusCode()).as(response.body()).isEqualTo(404);
        assertThat(response.body()).contains("\"error\"");
    }

    @Test
    void unknownProviderReturns400() throws Exception {
        HttpResponse<String> response = get("/api/v1/images?provider=no-such-provider");

        assertThat(response.statusCode()).as(response.body()).isEqualTo(400);
    }

    @Test
    void downloadStreamsRawBytesWithContentDisposition() throws Exception {
        HttpResponse<String> response = get("/api/v1/images/download/images:ubuntu/24.04?provider=fake");

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("fake-image-bytes");
        assertThat(response.headers().firstValue("Content-Disposition")).isPresent();
    }

    @Test
    void pullReturnsOperationJson() throws Exception {
        HttpResponse<String> response =
                postJson("/api/v1/images/pull", "{\"provider\":\"fake\",\"remote\":\"images\",\"identifier\":\"ubuntu/24.04\"}");

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        assertThat(response.body()).contains("SUCCEEDED");
    }

    @Test
    void importStreamsRawOctetStreamBody() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/images/import?provider=fake"))
                .header("Content-Type", "application/octet-stream")
                .POST(HttpRequest.BodyPublishers.ofByteArray("upload-bytes".getBytes(StandardCharsets.UTF_8)))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        assertThat(response.body()).contains("SUCCEEDED").contains("ubuntu/24.04");
    }

    @Test
    void createWorkloadReturnsWorkloadId() throws Exception {
        HttpResponse<String> response = postJson(
                "/api/v1/workloads",
                """
                {"provider":"fake","name":"ubuntu-test","type":"CONTAINER","image":{"remote":"images","name":"ubuntu/24.04"}}
                """);

        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        assertThat(response.body()).contains("ubuntu-test");
    }

    @Test
    void createVpsReconcilesToReadyAndIsGettable() throws Exception {
        HttpResponse<String> create = postJson(
                "/api/v1/vps",
                """
                {"name":"web-01","image":{"provider":"fake","remote":"images","name":"ubuntu/24.04"},"cpu":2,"memoryMb":2048}
                """);
        assertThat(create.statusCode()).as(create.body()).isEqualTo(200);
        assertThat(create.body()).contains("SUCCEEDED");
        String id = extractField(create.body(), "vpsId");

        HttpResponse<String> get = get("/api/v1/vps/" + id);

        assertThat(get.statusCode()).as(get.body()).isEqualTo(200);
        assertThat(get.body()).contains("\"state\":\"READY\"").contains("web-01");
    }

    @Test
    void invalidLifecycleTransitionReturns409() throws Exception {
        HttpResponse<String> create = postJson(
                "/api/v1/vps",
                """
                {"name":"web-02","image":{"provider":"fake","remote":"images","name":"ubuntu/24.04"}}
                """);
        String id = extractField(create.body(), "vpsId");

        // freshly created VPS is READY, not STOPPED — start() only accepts STOPPED.
        HttpResponse<String> response = postJson("/api/v1/vps/" + id + "/start", "");

        assertThat(response.statusCode()).as(response.body()).isEqualTo(409);
    }

    @Test
    void getUnknownVpsReturns404() throws Exception {
        HttpResponse<String> response = get("/api/v1/vps/vps-no-such-id");

        assertThat(response.statusCode()).as(response.body()).isEqualTo(404);
    }

    @Test
    void listGetAndCreateDnsRecordsForAPreRegisteredDomain() throws Exception {
        // TestApp.registerTestDomain pre-registers "example.com" at startup — there's no
        // POST /api/v1/domains endpoint (spec §37 has none); registration is a DomainManager-direct concern.
        HttpResponse<String> list = get("/api/v1/domains");
        assertThat(list.statusCode()).as(list.body()).isEqualTo(200);
        assertThat(list.body()).contains("example.com");

        HttpResponse<String> get = get("/api/v1/domains/example.com");
        assertThat(get.statusCode()).as(get.body()).isEqualTo(200);
        assertThat(get.body()).contains("\"dnsProvider\":\"cloudflare\"");

        HttpResponse<String> create = postJson(
                "/api/v1/domains/example.com/records", "{\"name\":\"app\",\"type\":\"A\",\"value\":\"203.0.113.10\"}");
        assertThat(create.statusCode()).as(create.body()).isEqualTo(200);
        assertThat(create.body()).contains("\"name\":\"app\"");

        HttpResponse<String> records = get("/api/v1/domains/example.com/records");
        assertThat(records.statusCode()).as(records.body()).isEqualTo(200);
        assertThat(records.body()).contains("app");
    }

    @Test
    void getUnknownDomainReturns404() throws Exception {
        HttpResponse<String> response = get("/api/v1/domains/unknown.com");

        assertThat(response.statusCode()).as(response.body()).isEqualTo(404);
    }

    @Test
    void requestCertificateThenGetRenewAndRevoke() throws Exception {
        HttpResponse<String> create = postJson(
                "/api/v1/certificates", "{\"domains\":[\"example.com\"],\"issuer\":\"letsencrypt\"}");
        assertThat(create.statusCode()).as(create.body()).isEqualTo(200);
        assertThat(create.body()).contains("\"status\":\"ACTIVE\"");
        assertThat(create.body()).doesNotContain("BEGIN PRIVATE KEY");
        String id = extractField(create.body(), "id");

        HttpResponse<String> get = get("/api/v1/certificates/" + id);
        assertThat(get.statusCode()).as(get.body()).isEqualTo(200);

        HttpResponse<String> renew = postJson("/api/v1/certificates/" + id + "/renew", "");
        assertThat(renew.statusCode()).as(renew.body()).isEqualTo(200);

        HttpResponse<String> revoke = postJson("/api/v1/certificates/" + id + "/revoke", "");
        assertThat(revoke.statusCode()).as(revoke.body()).isEqualTo(200);

        HttpResponse<String> afterRevoke = get("/api/v1/certificates/" + id);
        assertThat(afterRevoke.body()).contains("\"status\":\"REVOKED\"");
    }

    @Test
    void renewingARevokedCertificateReturns409() throws Exception {
        HttpResponse<String> create = postJson(
                "/api/v1/certificates", "{\"domains\":[\"example.com\"],\"issuer\":\"letsencrypt\"}");
        String id = extractField(create.body(), "id");
        postJson("/api/v1/certificates/" + id + "/revoke", "");

        HttpResponse<String> response = postJson("/api/v1/certificates/" + id + "/renew", "");

        assertThat(response.statusCode()).as(response.body()).isEqualTo(409);
    }

    private static String extractField(String json, String field) {
        String marker = "\"" + field + "\":\"";
        int start = json.indexOf(marker) + marker.length();
        return json.substring(start, json.indexOf('"', start));
    }

    @SpringBootApplication
    static class TestApp {

        @Bean
        ProviderRegistry providerRegistry() {
            return new ProviderRegistry(Map.of("fake", new FakeVirtualizationProvider()));
        }

        @Bean
        ImageProviderRegistry imageProviderRegistry() {
            return new ImageProviderRegistry(Map.of("fake", new FakeImageProvider()));
        }

        /** Pre-registers a domain and associates it with the configured "cloudflare" mock DNS provider — there's no POST /api/v1/domains endpoint (spec §37 has none), registration is a DomainManager-direct concern. */
        @Bean
        CommandLineRunner registerTestDomain(DomainManager domainManager) {
            return args -> {
                var domain = domainManager.register("example.com");
                domainManager.associateDnsProvider(domain.id(), "cloudflare");
            };
        }
    }
}
