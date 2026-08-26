package io.virtualization.sdk.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ComputeResourcesTest {

    @Test
    void constructsWithValidValues() {
        ComputeResources resources = new ComputeResources(4, 8192);

        assertThat(resources.cpuCores()).isEqualTo(4);
        assertThat(resources.memoryMb()).isEqualTo(8192);
    }

    @Test
    void rejectsNonPositiveCpuCores() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ComputeResources(0, 1024));
        assertThatIllegalArgumentException().isThrownBy(() -> new ComputeResources(-1, 1024));
    }

    @Test
    void rejectsNegativeMemory() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ComputeResources(1, -1));
    }
}
