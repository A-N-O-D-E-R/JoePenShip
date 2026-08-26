package io.virtualization.sdk.vps;

import io.virtualization.sdk.core.Operation;
import io.virtualization.sdk.core.OperationStatus;
import io.virtualization.sdk.core.image.ImageReference;
import io.virtualization.sdk.vps.support.FakeImageProvider;
import io.virtualization.sdk.vps.support.FakeVirtualizationProvider;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/** Wires {@link DefaultVpsManager} to the real {@link DefaultVpsProvisioner} — not just fakes on both sides. */
class VpsIntegrationTest {

    private static final ImageReference IMAGE = new ImageReference("fake", "images", "ubuntu/24.04");
    private static final ImageReference OTHER_IMAGE = new ImageReference("fake", "images", "debian/13");

    @Test
    void fullLifecycleThroughRealProvisioner() {
        FakeVirtualizationProvider virtualizationProvider = new FakeVirtualizationProvider();
        FakeImageProvider imageProvider = new FakeImageProvider().knows("ubuntu/24.04").knows("debian/13");
        VpsManager manager = new DefaultVpsManager(
                new InMemoryVpsRepository(), new DefaultVpsProvisioner(virtualizationProvider, imageProvider));

        CreateVpsOperation create = manager.create(VpsSpec.builder("web-01", IMAGE).cpu(2).memory(DataSize.ofGigabytes(4)).build());
        assertThat(create.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED);
        VpsId id = create.vpsId();
        assertThat(manager.get(id).state()).isEqualTo(VpsState.READY);
        String workloadId = manager.get(id).workloadId();
        assertThat(workloadId).isNotBlank();

        Operation stop = manager.stop(id);
        assertThat(stop.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED);
        assertThat(manager.get(id).state()).isEqualTo(VpsState.STOPPED);

        Operation start = manager.start(id);
        assertThat(start.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED);
        assertThat(manager.get(id).state()).isEqualTo(VpsState.RUNNING);

        // rebuild is only legal from READY/STOPPED/ERROR, not RUNNING — stop first.
        Operation stopBeforeRebuild = manager.stop(id);
        assertThat(stopBeforeRebuild.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED);
        assertThat(manager.get(id).state()).isEqualTo(VpsState.STOPPED);

        CreateVpsOperation rebuild = manager.rebuild(id, OTHER_IMAGE);
        assertThat(rebuild.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED);
        assertThat(manager.get(id).state()).isEqualTo(VpsState.READY);
        assertThat(manager.get(id).image()).isEqualTo(OTHER_IMAGE);
        assertThat(manager.get(id).workloadId()).isNotEqualTo(workloadId); // rebuild destroyed the old workload, created a new one

        Operation destroy = manager.destroy(id);
        assertThat(destroy.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.SUCCEEDED);
        assertThat(manager.get(id).state()).isEqualTo(VpsState.DESTROYED);

        assertThat(virtualizationProvider.calls()).contains("destroy:" + workloadId); // the old workload, destroyed by rebuild
    }

    @Test
    void createFailsFastForUnknownImageWithoutTouchingWorkloadProvider() {
        FakeVirtualizationProvider virtualizationProvider = new FakeVirtualizationProvider();
        FakeImageProvider imageProvider = new FakeImageProvider(); // knows nothing
        VpsManager manager = new DefaultVpsManager(
                new InMemoryVpsRepository(), new DefaultVpsProvisioner(virtualizationProvider, imageProvider));

        CreateVpsOperation operation = manager.create(VpsSpec.builder("web-01", IMAGE).build());

        assertThat(operation.await(Duration.ofSeconds(5))).isEqualTo(OperationStatus.FAILED);
        assertThat(manager.get(operation.vpsId()).state()).isEqualTo(VpsState.ERROR);
        assertThat(virtualizationProvider.calls()).isEmpty();
    }
}
