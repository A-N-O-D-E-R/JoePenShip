package io.virtualization.sdk.vps;

import io.virtualization.sdk.core.ComputeResources;
import io.virtualization.sdk.core.image.ImageReference;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class VpsTest {

    private static final ImageReference IMAGE = new ImageReference("incus", "images", "ubuntu/24.04");
    private static final ComputeResources COMPUTE = new ComputeResources(1, 1_024);
    private static final StorageConfiguration STORAGE = new StorageConfiguration(DataSize.ofGigabytes(10));
    private static final VpsSpec SPEC = VpsSpec.builder("web-01", IMAGE).build();
    private static final Instant NOW = Instant.now();

    @Test
    void requiredFieldsRejectNull() {
        assertThatNullPointerException().isThrownBy(() -> new Vps(
                null, "web-01", VpsState.READY, VpsType.VIRTUAL_MACHINE, IMAGE, COMPUTE, STORAGE,
                NetworkConfiguration.UNSPECIFIED, SPEC, null, null, null, NOW, NOW, null, null, null));
        assertThatNullPointerException().isThrownBy(() -> new Vps(
                VpsId.generate(), null, VpsState.READY, VpsType.VIRTUAL_MACHINE, IMAGE, COMPUTE, STORAGE,
                NetworkConfiguration.UNSPECIFIED, SPEC, null, null, null, NOW, NOW, null, null, null));
        assertThatNullPointerException().isThrownBy(() -> new Vps(
                VpsId.generate(), "web-01", null, VpsType.VIRTUAL_MACHINE, IMAGE, COMPUTE, STORAGE,
                NetworkConfiguration.UNSPECIFIED, SPEC, null, null, null, NOW, NOW, null, null, null));
        assertThatNullPointerException().isThrownBy(() -> new Vps(
                VpsId.generate(), "web-01", VpsState.READY, VpsType.VIRTUAL_MACHINE, null, COMPUTE, STORAGE,
                NetworkConfiguration.UNSPECIFIED, SPEC, null, null, null, NOW, NOW, null, null, null));
        assertThatNullPointerException().isThrownBy(() -> new Vps(
                VpsId.generate(), "web-01", VpsState.READY, VpsType.VIRTUAL_MACHINE, IMAGE, null, STORAGE,
                NetworkConfiguration.UNSPECIFIED, SPEC, null, null, null, NOW, NOW, null, null, null));
        assertThatNullPointerException().isThrownBy(() -> new Vps(
                VpsId.generate(), "web-01", VpsState.READY, VpsType.VIRTUAL_MACHINE, IMAGE, COMPUTE, null,
                NetworkConfiguration.UNSPECIFIED, SPEC, null, null, null, NOW, NOW, null, null, null));
        assertThatNullPointerException().isThrownBy(() -> new Vps(
                VpsId.generate(), "web-01", VpsState.READY, VpsType.VIRTUAL_MACHINE, IMAGE, COMPUTE, STORAGE, null,
                SPEC, null, null, null, NOW, NOW, null, null, null));
        assertThatNullPointerException().isThrownBy(() -> new Vps(
                VpsId.generate(), "web-01", VpsState.READY, VpsType.VIRTUAL_MACHINE, IMAGE, COMPUTE, STORAGE,
                NetworkConfiguration.UNSPECIFIED, null, null, null, null, NOW, NOW, null, null, null));
    }

    @Test
    void blankNameRejected() {
        assertThatIllegalArgumentException().isThrownBy(() -> new Vps(
                VpsId.generate(), "", VpsState.READY, VpsType.VIRTUAL_MACHINE, IMAGE, COMPUTE, STORAGE,
                NetworkConfiguration.UNSPECIFIED, SPEC, null, null, null, NOW, NOW, null, null, null));
    }

    @Test
    void providerProjectWorkloadIdAcceptNull() {
        Vps vps = new Vps(
                VpsId.generate(), "web-01", VpsState.PROVISIONING, VpsType.VIRTUAL_MACHINE, IMAGE, COMPUTE, STORAGE,
                NetworkConfiguration.UNSPECIFIED, SPEC, null, null, null, NOW, NOW, null, null, null);

        assertThat(vps.provider()).isNull();
        assertThat(vps.project()).isNull();
        assertThat(vps.workloadId()).isNull();
    }
}
