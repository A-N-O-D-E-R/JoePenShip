package io.virtualization.sdk.incus.client;

import io.virtualization.sdk.core.exception.ConnectionException;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.incus.IncusClientConfig;
import io.virtualization.sdk.incus.IncusTlsCredentials;
import io.virtualization.sdk.incus.client.dto.InstanceDto;
import io.virtualization.sdk.incus.support.FakeIncusServer;
import io.virtualization.sdk.incus.support.TestTlsFixtures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IncusApiClientTest {

    private FakeIncusServer server;

    @BeforeEach
    void startServer() {
        server = new FakeIncusServer().start();
        server.addInstance("vm-1", "virtual-machine", "Running", Map.of("limits.cpu", "2", "limits.memory", "2GiB"));
    }

    @AfterEach
    void stopServer() {
        server.close();
    }

    private IncusApiClient plainClient() {
        return new IncusApiClient(HttpClient.newHttpClient(), server.uri(), Duration.ofSeconds(5));
    }

    @Test
    void getListReturnsDeserializedInstances() {
        List<InstanceDto> instances = plainClient().getList("/instances", InstanceDto.class);

        assertThat(instances).hasSize(1);
        assertThat(instances.getFirst().name()).isEqualTo("vm-1");
        assertThat(instances.getFirst().isVirtualMachine()).isTrue();
    }

    @Test
    void getSingleReturnsDeserializedInstance() {
        InstanceDto instance = plainClient().getSingle("/instances/vm-1", InstanceDto.class);

        assertThat(instance.status()).isEqualTo("Running");
    }

    @Test
    void unknownInstanceRaisesResourceNotFoundException() {
        IncusApiClient client = plainClient();

        assertThatThrownBy(() -> client.getSingle("/instances/missing", InstanceDto.class))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void putForOperationIdReturnsGeneratedId() {
        String operationId = plainClient().putForOperationId("/instances/vm-1/state", "{\"action\":\"start\"}");

        assertThat(operationId).startsWith("op-");
    }

    @Test
    void unreachableServerRaisesConnectionException() {
        IncusApiClient client = new IncusApiClient(new IncusClientConfig(
                URI.create("https://localhost:1"),
                new IncusTlsCredentials(TestTlsFixtures.CLIENT_CERTIFICATE_PEM, TestTlsFixtures.CLIENT_KEY_PEM),
                "default",
                false,
                Duration.ofMillis(500),
                Duration.ofMillis(500),
                Duration.ofSeconds(1)));

        assertThatThrownBy(() -> client.getList("/instances", InstanceDto.class)).isInstanceOf(ConnectionException.class);
    }

    @Test
    void sslContextBuildsFromValidPkcs8Credentials() {
        IncusClientConfig config = IncusClientConfig.of(
                URI.create("https://localhost:1"),
                new IncusTlsCredentials(TestTlsFixtures.CLIENT_CERTIFICATE_PEM, TestTlsFixtures.CLIENT_KEY_PEM));

        assertThatCode(() -> new IncusApiClient(config)).doesNotThrowAnyException();
    }
}
