package io.virtualization.sdk.core;

import io.virtualization.sdk.core.exception.UnsupportedCapabilityException;
import io.virtualization.sdk.core.image.ImageReference;
import io.virtualization.sdk.core.image.WorkloadSpec;
import io.virtualization.sdk.core.image.WorkloadType;
import io.virtualization.sdk.core.support.FakeVirtualizationProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UnsupportedCapabilityBehaviorTest {

    @Test
    void unsupportedCapabilityThrowsDedicatedException() {
        VirtualizationProvider provider = new FakeVirtualizationProvider(ProviderCapabilities.of(Capability.START));

        assertThatThrownBy(() -> provider.stop("vm-1")).isInstanceOf(UnsupportedCapabilityException.class);
    }

    @Test
    void supportedCapabilityReturnsOperation() {
        VirtualizationProvider provider = new FakeVirtualizationProvider(ProviderCapabilities.of(Capability.START));

        Operation operation = provider.start("vm-1");

        assertThat(operation.status()).isEqualTo(OperationStatus.SUCCEEDED);
    }

    @Test
    void createFromImageDefaultsToUnsupported() {
        VirtualizationProvider provider = new FakeVirtualizationProvider(ProviderCapabilities.of(Capability.START));
        ImageReference image = new ImageReference("fake", "images", "ubuntu/24.04");
        WorkloadSpec spec = WorkloadSpec.builder("ubuntu-test", WorkloadType.CONTAINER).build();

        assertThatThrownBy(() -> provider.createFromImage(image, spec)).isInstanceOf(UnsupportedCapabilityException.class);
    }
}
