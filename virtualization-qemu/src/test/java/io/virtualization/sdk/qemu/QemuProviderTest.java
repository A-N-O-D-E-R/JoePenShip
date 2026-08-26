package io.virtualization.sdk.qemu;

import io.virtualization.sdk.core.Operation;
import io.virtualization.sdk.core.OperationStatus;
import io.virtualization.sdk.core.VirtualMachine;
import io.virtualization.sdk.core.VirtualMachineState;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.core.exception.UnsupportedCapabilityException;
import io.virtualization.sdk.qemu.qmp.QmpClient;
import io.virtualization.sdk.qemu.qmp.TcpEndpoint;
import io.virtualization.sdk.qemu.qmp.support.FakeQmpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QemuProviderTest {

    private FakeQmpServer server;
    private QmpClient client;
    private QemuProvider provider;

    @BeforeEach
    void startServer() {
        server = new FakeQmpServer();
        server.onCommand("query-status", req -> "{\"return\":{\"status\":\"running\"}}");
        server.onCommand("query-cpus", req -> "{\"return\":[{\"CPU\":0},{\"CPU\":1}]}");
        client = QmpClient.connect(new TcpEndpoint("localhost", server.port()), Duration.ofSeconds(2), Duration.ofSeconds(2));
        provider = new QemuProvider("vm-1", client);
    }

    @AfterEach
    void stopServer() {
        client.close();
        server.close();
    }

    @Test
    void listVirtualMachinesReturnsTheConfiguredVm() {
        List<VirtualMachine> vms = provider.listVirtualMachines();

        assertThat(vms).hasSize(1);
        VirtualMachine vm = vms.getFirst();
        assertThat(vm.id()).isEqualTo("vm-1");
        assertThat(vm.state()).isEqualTo(VirtualMachineState.RUNNING);
        assertThat(vm.resources().cpuCores()).isEqualTo(2);
    }

    @Test
    void getVirtualMachineThrowsForAnyOtherId() {
        assertThatThrownBy(() -> provider.getVirtualMachine("vm-2")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void startCompletesSuccessfully() {
        Operation operation = provider.start("vm-1");

        assertThat(operation.status()).isEqualTo(OperationStatus.SUCCEEDED);
    }

    @Test
    void stopReportsFailureWhenQmpReturnsAnError() {
        server.respondWithError("stop", "GenericError", "guest agent unavailable");

        Operation operation = provider.stop("vm-1");

        assertThat(operation.status()).isEqualTo(OperationStatus.FAILED);
        assertThat(operation.error()).isPresent();
    }

    @Test
    void rebootAndShutdownComplete() {
        assertThat(provider.reboot("vm-1").status()).isEqualTo(OperationStatus.SUCCEEDED);
        assertThat(provider.shutdown("vm-1").status()).isEqualTo(OperationStatus.SUCCEEDED);
    }

    @Test
    void destroyIsUnsupported() {
        assertThatThrownBy(() -> provider.destroy("vm-1")).isInstanceOf(UnsupportedCapabilityException.class);
    }

    @Test
    void lifecycleOperationOnUnknownVmThrowsResourceNotFound() {
        assertThatThrownBy(() -> provider.start("vm-2")).isInstanceOf(ResourceNotFoundException.class);
    }
}
