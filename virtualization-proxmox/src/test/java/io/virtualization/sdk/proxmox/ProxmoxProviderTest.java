package io.virtualization.sdk.proxmox;

import io.virtualization.sdk.core.Operation;
import io.virtualization.sdk.core.OperationStatus;
import io.virtualization.sdk.core.VirtualMachine;
import io.virtualization.sdk.core.VirtualMachineState;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.proxmox.client.ProxmoxApiClient;
import io.virtualization.sdk.proxmox.support.FakeProxmoxServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProxmoxProviderTest {

    private static final ProxmoxCredentials CREDENTIALS = new ProxmoxCredentials("root@pam!sdk", "token-secret");

    private FakeProxmoxServer server;
    private ProxmoxProvider provider;

    @BeforeEach
    void startServer() {
        server = new FakeProxmoxServer("PVEAPIToken=root@pam!sdk=token-secret").start();
        server.addVm(100, "pve1", "web-1", "running", 2, 2_147_483_648L);
        server.pollsBeforeCompletion(2);

        ProxmoxApiClient client = new ProxmoxApiClient(ProxmoxClientConfig.of(server.uri(), CREDENTIALS));
        provider = new ProxmoxProvider(client, Duration.ofMillis(10));
    }

    @AfterEach
    void stopServer() {
        server.close();
    }

    @Test
    void listVirtualMachinesMapsProxmoxResources() {
        List<VirtualMachine> vms = provider.listVirtualMachines();

        assertThat(vms).hasSize(1);
        VirtualMachine vm = vms.getFirst();
        assertThat(vm.id()).isEqualTo("100");
        assertThat(vm.name()).isEqualTo("web-1");
        assertThat(vm.state()).isEqualTo(VirtualMachineState.RUNNING);
        assertThat(vm.resources().cpuCores()).isEqualTo(2);
        assertThat(vm.resources().memoryMb()).isEqualTo(2048);
    }

    @Test
    void getVirtualMachineReturnsMatchingVm() {
        VirtualMachine vm = provider.getVirtualMachine("100");

        assertThat(vm.id()).isEqualTo("100");
    }

    @Test
    void getVirtualMachineThrowsForUnknownId() {
        assertThatThrownBy(() -> provider.getVirtualMachine("999")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void startCompletesSuccessfullyAfterTaskFinishes() {
        Operation operation = provider.start("100");

        assertThat(operation.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED);
        assertThat(operation.error()).isEmpty();
    }

    @Test
    void stopReportsFailureWhenTaskFails() {
        server.nextTaskFails();

        Operation operation = provider.stop("100");

        assertThat(operation.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.FAILED);
        assertThat(operation.error()).isPresent();
    }

    @Test
    void rebootShutdownAndDestroyAllComplete() {
        assertThat(provider.reboot("100").await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED);
        assertThat(provider.shutdown("100").await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED);
        assertThat(provider.destroy("100").await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED);
    }

    @Test
    void lifecycleOperationOnUnknownVmThrowsResourceNotFound() {
        assertThatThrownBy(() -> provider.start("999")).isInstanceOf(ResourceNotFoundException.class);
    }
}
