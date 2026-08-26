package io.virtualization.sdk.vps;

import io.virtualization.sdk.core.ComputeResources;
import io.virtualization.sdk.core.Operation;
import io.virtualization.sdk.core.OperationStatus;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.core.image.ImageReference;
import io.virtualization.sdk.core.image.WorkloadSpec;
import io.virtualization.sdk.core.image.WorkloadType;
import io.virtualization.sdk.vps.support.FakeImageProvider;
import io.virtualization.sdk.vps.support.FakeVirtualizationProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultVpsProvisionerTest {

    private static final ImageReference IMAGE = new ImageReference("fake", "images", "ubuntu/24.04");
    private static final ImageReference OTHER_IMAGE = new ImageReference("fake", "images", "debian/13");

    private FakeVirtualizationProvider virtualizationProvider;
    private FakeImageProvider imageProvider;
    private DefaultVpsProvisioner provisioner;

    @BeforeEach
    void setUp() {
        virtualizationProvider = new FakeVirtualizationProvider();
        imageProvider = new FakeImageProvider().knows("ubuntu/24.04").knows("debian/13");
        provisioner = new DefaultVpsProvisioner(virtualizationProvider, imageProvider);
    }

    @Test
    void createSucceedsAndMapsSpecToWorkloadSpec() {
        VpsSpec spec = VpsSpec.builder("web-01", IMAGE)
                .type(VpsType.CONTAINER)
                .cpu(2)
                .memory(DataSize.ofGigabytes(4))
                .disk(DataSize.ofGigabytes(40))
                .storagePool("fast")
                .volumeType("ssd")
                .network(new NetworkConfiguration("default", "10.0.0.5", null, "web-01"))
                .sshPublicKeys(List.of("ssh-ed25519 AAAA"))
                .cloudInit("#cloud-config\n")
                .location("us-east")
                .project("prod")
                .build();

        CreateVpsOperation operation = provisioner.create(VpsId.generate(), spec);

        assertThat(operation.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED);
        assertThat(operation.vps()).isPresent();
        assertThat(operation.vps().get().provider()).isEqualTo("fake");
        assertThat(operation.vps().get().workloadId()).isEqualTo("workload-1");

        WorkloadSpec workloadSpec = virtualizationProvider.lastWorkloadSpec();
        assertThat(workloadSpec.name()).isEqualTo("web-01");
        assertThat(workloadSpec.type()).isEqualTo(WorkloadType.CONTAINER);
        assertThat(workloadSpec.image()).contains(IMAGE);
        assertThat(workloadSpec.resources()).contains(new ComputeResources(2, 4_096));
        assertThat(workloadSpec.storage()).containsExactly("root:40960MB");
        assertThat(workloadSpec.networks()).containsExactly("default");
        assertThat(workloadSpec.providerOptions().get(VpsProviderOptionKeys.SSH_PUBLIC_KEYS)).contains(List.of("ssh-ed25519 AAAA"));
        assertThat(workloadSpec.providerOptions().getString(VpsProviderOptionKeys.CLOUD_INIT)).contains("#cloud-config\n");
        assertThat(workloadSpec.providerOptions().getString(VpsProviderOptionKeys.HOSTNAME)).contains("web-01");
        assertThat(workloadSpec.providerOptions().getString(VpsProviderOptionKeys.IPV4)).contains("10.0.0.5");
        assertThat(workloadSpec.providerOptions().getString(VpsProviderOptionKeys.STORAGE_POOL)).contains("fast");
        assertThat(workloadSpec.providerOptions().getString(VpsProviderOptionKeys.VOLUME_TYPE)).contains("ssd");
        assertThat(workloadSpec.providerOptions().getString(VpsProviderOptionKeys.LOCATION)).contains("us-east");
        assertThat(workloadSpec.providerOptions().getString(VpsProviderOptionKeys.PROJECT)).contains("prod");
    }

    @Test
    void createFailsFastWhenImageUnknown() {
        VpsSpec spec = VpsSpec.builder("web-01", new ImageReference("fake", "images", "missing")).build();

        CreateVpsOperation operation = provisioner.create(VpsId.generate(), spec);

        assertThat(operation.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.FAILED);
        assertThat(operation.error()).containsInstanceOf(ResourceNotFoundException.class);
        assertThat(virtualizationProvider.calls()).isEmpty();
    }

    @Test
    void createFailsWhenWorkloadCreationFails() {
        virtualizationProvider.createFails();
        VpsSpec spec = VpsSpec.builder("web-01", IMAGE).build();

        CreateVpsOperation operation = provisioner.create(VpsId.generate(), spec);

        assertThat(operation.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.FAILED);
        assertThat(operation.vps()).isEmpty();
    }

    @Test
    void rebuildDestroysOldWorkloadThenCreatesFromNewImage() {
        VpsId id = VpsId.generate();
        Vps current = sampleVps(id, "web-01", "workload-old");

        CreateVpsOperation operation = provisioner.rebuild(id, current, OTHER_IMAGE);

        assertThat(operation.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED);
        assertThat(virtualizationProvider.calls()).containsExactly("destroy:workload-old", "createFromImage:web-01");
        assertThat(virtualizationProvider.lastImageReference()).isEqualTo(OTHER_IMAGE);
        assertThat(operation.vps().orElseThrow().image()).isEqualTo(OTHER_IMAGE);
    }

    @Test
    void rebuildWithNoExistingWorkloadSkipsDestroy() {
        VpsId id = VpsId.generate();
        Vps current = sampleVps(id, "web-01", null);

        CreateVpsOperation operation = provisioner.rebuild(id, current, OTHER_IMAGE);

        assertThat(operation.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED);
        assertThat(virtualizationProvider.calls()).containsExactly("createFromImage:web-01");
    }

    @Test
    void rebuildFailsFastWhenNewImageUnknown() {
        VpsId id = VpsId.generate();
        Vps current = sampleVps(id, "web-01", "workload-old");

        CreateVpsOperation operation = provisioner.rebuild(id, current, new ImageReference("fake", "images", "missing"));

        assertThat(operation.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.FAILED);
        assertThat(virtualizationProvider.calls()).isEmpty();
    }

    @Test
    void lifecycleMethodsDelegateToVirtualizationProviderByWorkloadId() {
        VpsId id = VpsId.generate();
        Vps current = sampleVps(id, "web-01", "workload-1");

        provisioner.start(id, current);
        provisioner.stop(id, current);
        provisioner.restart(id, current);
        provisioner.shutdown(id, current);
        provisioner.destroy(id, current);

        assertThat(virtualizationProvider.calls()).containsExactly(
                "start:workload-1", "stop:workload-1", "reboot:workload-1", "shutdown:workload-1", "destroy:workload-1");
    }

    @Test
    void lifecycleMethodsThrowWhenNoWorkloadYet() {
        VpsId id = VpsId.generate();
        Vps current = sampleVps(id, "web-01", null);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> provisioner.start(id, current))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void lifecycleFailurePropagatesAsFailedOperation() {
        virtualizationProvider.lifecycleFails();
        VpsId id = VpsId.generate();
        Vps current = sampleVps(id, "web-01", "workload-1");

        Operation operation = provisioner.stop(id, current);

        assertThat(operation.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.FAILED);
    }

    private static Vps sampleVps(VpsId id, String name, String workloadId) {
        Instant now = Instant.now();
        VpsSpec spec = VpsSpec.builder(name, IMAGE).build();
        return new Vps(
                id, name, VpsState.READY, VpsType.VIRTUAL_MACHINE, IMAGE, new ComputeResources(1, 1_024),
                new StorageConfiguration(DataSize.ofGigabytes(10)), NetworkConfiguration.UNSPECIFIED, spec, "fake",
                null, workloadId, now, now, null, null, null);
    }
}
