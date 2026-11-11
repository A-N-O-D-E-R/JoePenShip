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

    @Test
    void applyFirewallDefaultsToUnsupported() {
        VirtualizationProvider provider = new FakeVirtualizationProvider(ProviderCapabilities.of(Capability.START));
        var firewall = new io.virtualization.sdk.core.firewall.WorkloadFirewall(java.util.List.of());

        assertThatThrownBy(() -> provider.applyFirewall("vm-1", firewall)).isInstanceOf(UnsupportedCapabilityException.class);
    }

    @Test
    void firewallRulesValidateTheirShape() {
        assertThatThrownBy(() -> new io.virtualization.sdk.core.firewall.WorkloadFirewall.Rule(
                io.virtualization.sdk.core.firewall.WorkloadFirewall.Direction.INBOUND, "tcp", 443, 80,
                java.util.List.of("0.0.0.0/0"), null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new io.virtualization.sdk.core.firewall.WorkloadFirewall.Rule(
                io.virtualization.sdk.core.firewall.WorkloadFirewall.Direction.INBOUND, null, null, null,
                java.util.List.of(), null)).isInstanceOf(IllegalArgumentException.class);
    }
}
