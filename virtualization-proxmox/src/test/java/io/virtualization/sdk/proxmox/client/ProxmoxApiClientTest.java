package io.virtualization.sdk.proxmox.client;

import io.virtualization.sdk.core.exception.AuthenticationException;
import io.virtualization.sdk.core.exception.ConnectionException;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.proxmox.ProxmoxClientConfig;
import io.virtualization.sdk.proxmox.ProxmoxCredentials;
import io.virtualization.sdk.proxmox.client.dto.ClusterResourceDto;
import io.virtualization.sdk.proxmox.support.FakeProxmoxServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProxmoxApiClientTest {

    private static final ProxmoxCredentials CREDENTIALS = new ProxmoxCredentials("root@pam!sdk", "token-secret");

    private FakeProxmoxServer server;

    @BeforeEach
    void startServer() {
        server = new FakeProxmoxServer("PVEAPIToken=root@pam!sdk=token-secret").start();
        server.addVm(100, "pve1", "web-1", "running", 2, 2_147_483_648L);
    }

    @AfterEach
    void stopServer() {
        server.close();
    }

    private ProxmoxApiClient client() {
        return new ProxmoxApiClient(ProxmoxClientConfig.of(server.uri(), CREDENTIALS));
    }

    @Test
    void getListReturnsDeserializedResources() {
        List<ClusterResourceDto> resources = client().getList("/cluster/resources?type=vm", ClusterResourceDto.class);

        assertThat(resources).hasSize(1);
        assertThat(resources.getFirst().vmid()).isEqualTo(100);
        assertThat(resources.getFirst().node()).isEqualTo("pve1");
    }

    @Test
    void postForTaskIdReturnsUpid() {
        String upid = client().postForTaskId("/nodes/pve1/qemu/100/status/start");

        assertThat(upid).startsWith("UPID:pve1:");
    }

    @Test
    void wrongTokenRaisesAuthenticationException() {
        ProxmoxCredentials wrong = new ProxmoxCredentials("root@pam!sdk", "wrong-secret");
        ProxmoxApiClient client = new ProxmoxApiClient(ProxmoxClientConfig.of(server.uri(), wrong));

        assertThatThrownBy(() -> client.getList("/cluster/resources?type=vm", ClusterResourceDto.class))
                .isInstanceOf(AuthenticationException.class);
    }

    @Test
    void unknownResourceRaisesResourceNotFoundException() {
        assertThatThrownBy(() -> client().postForTaskId("/nodes/pve1/qemu/999/status/start"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void unreachableServerRaisesConnectionException() {
        ProxmoxApiClient client = new ProxmoxApiClient(new ProxmoxClientConfig(
                URI.create("http://localhost:1"),
                CREDENTIALS,
                true,
                Duration.ofMillis(500),
                Duration.ofMillis(500),
                Duration.ofSeconds(1)));

        assertThatThrownBy(() -> client.getList("/cluster/resources?type=vm", ClusterResourceDto.class))
                .isInstanceOf(ConnectionException.class);
    }
}
