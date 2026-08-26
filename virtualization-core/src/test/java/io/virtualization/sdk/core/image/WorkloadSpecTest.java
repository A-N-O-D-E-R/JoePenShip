package io.virtualization.sdk.core.image;

import io.virtualization.sdk.core.ComputeResources;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class WorkloadSpecTest {

    @Test
    void minimalSpecHasEmptyDefaults() {
        WorkloadSpec spec = WorkloadSpec.builder("ubuntu-test", WorkloadType.CONTAINER).build();

        assertThat(spec.name()).isEqualTo("ubuntu-test");
        assertThat(spec.type()).isEqualTo(WorkloadType.CONTAINER);
        assertThat(spec.image()).isEmpty();
        assertThat(spec.resources()).isEmpty();
        assertThat(spec.storage()).isEmpty();
        assertThat(spec.networks()).isEmpty();
        assertThat(spec.environment()).isEmpty();
        assertThat(spec.providerOptions().asMap()).isEmpty();
    }

    @Test
    void builderPopulatesGivenFields() {
        ImageReference image = new ImageReference("incus", "images", "ubuntu/24.04");
        WorkloadSpec spec = WorkloadSpec.builder("ubuntu-vm", WorkloadType.VIRTUAL_MACHINE)
                .image(image)
                .architecture("x86_64")
                .resources(new ComputeResources(2, 2048))
                .storage(java.util.List.of("root:20GB"))
                .networks(java.util.List.of("default"))
                .environment(Map.of("FOO", "bar"))
                .providerOptions(ProviderOptions.of(Map.of("profile", "gpu")))
                .build();

        assertThat(spec.image()).contains(image);
        assertThat(spec.architecture()).contains("x86_64");
        assertThat(spec.resources()).contains(new ComputeResources(2, 2048));
        assertThat(spec.storage()).containsExactly("root:20GB");
        assertThat(spec.networks()).containsExactly("default");
        assertThat(spec.environment()).containsEntry("FOO", "bar");
        assertThat(spec.providerOptions().getString("profile")).contains("gpu");
    }

    @Test
    void rejectsBlankOrNullName() {
        assertThatIllegalArgumentException().isThrownBy(() -> WorkloadSpec.builder("", WorkloadType.CONTAINER).build());
        assertThatNullPointerException().isThrownBy(() -> WorkloadSpec.builder(null, WorkloadType.CONTAINER).build());
        assertThatNullPointerException().isThrownBy(() -> WorkloadSpec.builder("name", null).build());
    }
}
