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

import io.virtualization.sdk.core.ComputeResources;
import io.virtualization.sdk.core.image.ProviderOptions;
import io.virtualization.sdk.core.image.CreateWorkloadOperation;
import io.virtualization.sdk.core.image.ImageReference;
import io.virtualization.sdk.core.image.WorkloadSpec;
import io.virtualization.sdk.core.image.WorkloadType;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

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

    private static WorkloadSpec spec(String name, long diskGb, Map<String, String> configuration) {
        return WorkloadSpec.builder(name, WorkloadType.VIRTUAL_MACHINE)
                .resources(new ComputeResources(2, 4096))
                .configuration(configuration)
                .labels(Map.of("stratus-instance", name))
                .providerOptions(ProviderOptions.of(Map.of("node", "pve2", "diskGb", diskGb)))
                .build();
    }

    @Test
    void createFromImageClonesConfiguresAndGrowsTheDisk() {
        server.addTemplate(9000, "pve1", "debian-13", "10G");
        String key = "ssh-ed25519 AAAAC3Nza test@stratus";

        CreateWorkloadOperation op = provider.createFromImage(new ImageReference("proxmox", "9000"),
                spec("i-0abc", 20, Map.of("sshAuthorizedKeys", key)));

        assertThat(op.await(Duration.ofSeconds(5))).as(op.error().map(Throwable::getMessage).orElse("")).isEqualTo(OperationStatus.SUCCEEDED);
        String vmid = op.workloadId().orElseThrow();
        assertThat(vmid).isEqualTo("9001");
        VirtualMachine vm = provider.getVirtualMachine(vmid);
        assertThat(vm.name()).isEqualTo("i-0abc");
        assertThat(vm.state()).isEqualTo(VirtualMachineState.STOPPED);
        assertThat(vm.resources().cpuCores()).isEqualTo(2);
        assertThat(vm.resources().memoryMb()).isEqualTo(4096);

        Map<String, String> config = server.config(9001);
        // Proxmox stores sshkeys URL-encoded; decoding gives the key back.
        assertThat(URLDecoder.decode(config.get("sshkeys"), StandardCharsets.UTF_8)).isEqualTo(key + "\n");
        assertThat(config.get("sshkeys")).doesNotContain("+").contains("%20");
        assertThat(config.get("scsi0")).endsWith("size=20G"); // boot order skips the cloud-init CD-ROM
        assertThat(config.get("description")).isEqualTo("stratus-instance=i-0abc");
    }

    @Test
    void createFromImageNeverShrinksAndRetriesAnIdCollision() {
        server.addTemplate(9000, "pve1", "debian-13", "30G");
        server.nextIdCollides(1);

        CreateWorkloadOperation op = provider.createFromImage(new ImageReference("proxmox", "9000"), spec("i-0def", 20, Map.of()));

        assertThat(op.await(Duration.ofSeconds(5))).as(op.error().map(Throwable::getMessage).orElse("")).isEqualTo(OperationStatus.SUCCEEDED);
        assertThat(server.config(Integer.parseInt(op.workloadId().orElseThrow())).get("scsi0")).endsWith("size=30G");
    }

    @Test
    void createFromImageReusesTheVmOfAnInterruptedAttempt() {
        server.addTemplate(9000, "pve1", "debian-13", "10G");
        var first = provider.createFromImage(new ImageReference("proxmox", "9000"), spec("i-0retry", 10, Map.of()));
        assertThat(first.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED);

        var again = provider.createFromImage(new ImageReference("proxmox", "9000"), spec("i-0retry", 10, Map.of()));

        assertThat(again.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED);
        assertThat(again.workloadId()).isEqualTo(first.workloadId());
        assertThat(provider.listVirtualMachines()).filteredOn(vm -> vm.name().equals("i-0retry")).hasSize(1);
    }

    @Test
    void createFromImageFailsWhenTheCloneTaskFails() {
        server.addTemplate(9000, "pve1", "debian-13", "10G");
        server.nextTaskFails();

        var op = provider.createFromImage(new ImageReference("proxmox", "9000"), spec("i-0fail", 10, Map.of()));

        assertThat(op.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.FAILED);
        assertThat(op.error()).isPresent();
        assertThatThrownBy(() -> provider.createFromImage(new ImageReference("proxmox", "debian"), spec("x", 10, Map.of())))
                .isInstanceOf(IllegalArgumentException.class);
        var missing = provider.createFromImage(new ImageReference("proxmox", "1234"), spec("y", 10, Map.of()));
        assertThat(missing.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.FAILED);
        assertThat(missing.error()).get().isInstanceOf(ResourceNotFoundException.class);
    }
}
