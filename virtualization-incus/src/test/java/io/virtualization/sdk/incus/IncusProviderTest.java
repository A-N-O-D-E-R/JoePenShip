package io.virtualization.sdk.incus;

import io.virtualization.sdk.core.Container;
import io.virtualization.sdk.core.Operation;
import io.virtualization.sdk.core.OperationStatus;
import io.virtualization.sdk.core.VirtualMachine;
import io.virtualization.sdk.core.VirtualMachineState;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.incus.client.IncusApiClient;
import io.virtualization.sdk.incus.support.FakeIncusServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IncusProviderTest {

    private FakeIncusServer server;
    private IncusProvider provider;

    @BeforeEach
    void startServer() {
        server = new FakeIncusServer().start();
        server.addInstance("vm-1", "virtual-machine", "Running", Map.of("limits.cpu", "2", "limits.memory", "2GiB"));
        server.addInstance("ct-1", "container", "Running", Map.of());
        server.pollsBeforeCompletion(2);

        IncusApiClient client = new IncusApiClient(HttpClient.newHttpClient(), server.uri(), Duration.ofSeconds(5));
        provider = new IncusProvider(client, Duration.ofSeconds(1));
    }

    @AfterEach
    void stopServer() {
        server.close();
    }

    @Test
    void listVirtualMachinesExcludesContainers() {
        List<VirtualMachine> vms = provider.listVirtualMachines();

        assertThat(vms).hasSize(1);
        VirtualMachine vm = vms.getFirst();
        assertThat(vm.id()).isEqualTo("vm-1");
        assertThat(vm.state()).isEqualTo(VirtualMachineState.RUNNING);
        assertThat(vm.resources().cpuCores()).isEqualTo(2);
        assertThat(vm.resources().memoryMb()).isEqualTo(2048);
    }

    @Test
    void getVirtualMachineReturnsMatchingVm() {
        VirtualMachine vm = provider.getVirtualMachine("vm-1");

        assertThat(vm.id()).isEqualTo("vm-1");
    }

    @Test
    void getVirtualMachineThrowsForContainerId() {
        assertThatThrownBy(() -> provider.getVirtualMachine("ct-1")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getVirtualMachineThrowsForUnknownId() {
        assertThatThrownBy(() -> provider.getVirtualMachine("missing")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listContainersExcludesVirtualMachines() {
        List<Container> containers = provider.listContainers();

        assertThat(containers).hasSize(1);
        assertThat(containers.getFirst().id()).isEqualTo("ct-1");
    }

    @Test
    void getContainerReturnsMatchingContainer() {
        Container container = provider.getContainer("ct-1");

        assertThat(container.id()).isEqualTo("ct-1");
    }

    @Test
    void getContainerThrowsForVirtualMachineId() {
        assertThatThrownBy(() -> provider.getContainer("vm-1")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void startCompletesSuccessfullyAfterOperationFinishes() {
        Operation operation = provider.start("vm-1");

        assertThat(operation.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED);
        assertThat(operation.error()).isEmpty();
    }

    @Test
    void stopReportsFailureWhenOperationFails() {
        server.nextOperationFails();

        Operation operation = provider.stop("vm-1");

        assertThat(operation.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.FAILED);
        assertThat(operation.error()).isPresent();
    }

    @Test
    void rebootShutdownAndDestroyAllComplete() {
        assertThat(provider.reboot("vm-1").await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED);
        assertThat(provider.shutdown("vm-1").await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED);
        assertThat(provider.destroy("vm-1").await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED);
    }

    @Test
    void lifecycleOperationOnUnknownVmThrowsResourceNotFound() {
        assertThatThrownBy(() -> provider.start("missing")).isInstanceOf(ResourceNotFoundException.class);
    }
}
