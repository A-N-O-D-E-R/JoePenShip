package io.virtualization.sdk.vps;

import io.virtualization.sdk.core.ComputeResources;
import io.virtualization.sdk.core.image.ImageReference;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class VpsSpecTest {

    private static final ImageReference IMAGE = new ImageReference("incus", "images", "ubuntu/24.04");

    @Test
    void requiresNameAndImage() {
        assertThatNullPointerException().isThrownBy(() -> VpsSpec.builder(null, IMAGE).build());
        assertThatIllegalArgumentException().isThrownBy(() -> VpsSpec.builder("", IMAGE).build());
        assertThatNullPointerException().isThrownBy(() -> VpsSpec.builder("web-01", null).build());
    }

    @Test
    void defaults() {
        VpsSpec spec = VpsSpec.builder("web-01", IMAGE).build();

        assertThat(spec.type()).isEqualTo(VpsType.VIRTUAL_MACHINE);
        assertThat(spec.compute()).isEmpty();
        assertThat(spec.storage()).isEmpty();
        assertThat(spec.network()).isEmpty();
        assertThat(spec.sshPublicKeys()).isEmpty();
        assertThat(spec.cloudInit()).isEmpty();
        assertThat(spec.metadata()).isEmpty();
        assertThat(spec.labels()).isEmpty();
        assertThat(spec.location()).isEmpty();
        assertThat(spec.project()).isEmpty();
        assertThat(spec.idempotencyKey()).isEmpty();
    }

    @Test
    void cpuAndMemorySugarBuildComputeResources() {
        VpsSpec spec = VpsSpec.builder("web-01", IMAGE).cpu(2).memory(DataSize.ofGigabytes(4)).build();

        assertThat(spec.compute()).contains(new ComputeResources(2, 4_096));
    }

    @Test
    void wholeObjectComputeWinsOverSugar() {
        VpsSpec spec = VpsSpec.builder("web-01", IMAGE)
                .cpu(2)
                .memory(DataSize.ofGigabytes(4))
                .compute(new ComputeResources(8, 16_384))
                .build();

        assertThat(spec.compute()).contains(new ComputeResources(8, 16_384));
    }

    @Test
    void diskSugarBuildsStorageConfiguration() {
        VpsSpec spec = VpsSpec.builder("web-01", IMAGE).disk(DataSize.ofGigabytes(40)).storagePool("fast").build();

        assertThat(spec.storage()).contains(new StorageConfiguration(DataSize.ofGigabytes(40), "fast", null));
    }

    @Test
    void wholeObjectStorageWinsOverSugar() {
        StorageConfiguration explicit = new StorageConfiguration(DataSize.ofGigabytes(100), "custom", "ssd");
        VpsSpec spec = VpsSpec.builder("web-01", IMAGE).disk(DataSize.ofGigabytes(40)).storage(explicit).build();

        assertThat(spec.storage()).contains(explicit);
    }

    @Test
    void everyOtherSetterRoundTrips() {
        NetworkConfiguration network = new NetworkConfiguration("default", null, null, "web-01");
        VpsSpec spec = VpsSpec.builder("web-01", IMAGE)
                .type(VpsType.CONTAINER)
                .network(network)
                .sshPublicKeys(List.of("ssh-ed25519 AAAA"))
                .cloudInit("#cloud-config\n")
                .metadata(Map.of("k", "v"))
                .labels(Map.of("env", "prod"))
                .location("us-east")
                .project("default")
                .idempotencyKey("key-1")
                .build();

        assertThat(spec.type()).isEqualTo(VpsType.CONTAINER);
        assertThat(spec.network()).contains(network);
        assertThat(spec.sshPublicKeys()).containsExactly("ssh-ed25519 AAAA");
        assertThat(spec.cloudInit()).contains("#cloud-config\n");
        assertThat(spec.metadata()).containsEntry("k", "v");
        assertThat(spec.labels()).containsEntry("env", "prod");
        assertThat(spec.location()).contains("us-east");
        assertThat(spec.project()).contains("default");
        assertThat(spec.idempotencyKey()).contains("key-1");
    }
}
