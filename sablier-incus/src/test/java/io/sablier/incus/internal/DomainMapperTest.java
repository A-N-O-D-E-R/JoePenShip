package io.sablier.incus.internal;

import io.sablier.core.Workload;
import io.sablier.core.WorkloadMetadata;
import io.sablier.core.WorkloadState;
import io.sablier.core.WorkloadType;
import io.virtualization.sdk.core.ComputeResources;
import io.virtualization.sdk.core.Container;
import io.virtualization.sdk.core.VirtualMachine;
import io.virtualization.sdk.core.VirtualMachineState;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DomainMapperTest {

    private static final WorkloadMetadata METADATA = new WorkloadMetadata(true, "media", Optional.empty(), Optional.empty());
    private static final ComputeResources RESOURCES = new ComputeResources(1, 512);

    @Test
    void mapsVirtualMachineToWorkload() {
        VirtualMachine vm = new VirtualMachine(
                "jellyfin", "jellyfin", VirtualMachineState.RUNNING, RESOURCES, Map.of(), Optional.of("node1"));

        Workload workload = DomainMapper.toWorkload("default", vm, METADATA);

        assertThat(workload.id()).isEqualTo("jellyfin");
        assertThat(workload.type()).isEqualTo(WorkloadType.VIRTUAL_MACHINE);
        assertThat(workload.state()).isEqualTo(WorkloadState.RUNNING);
        assertThat(workload.group()).isEqualTo("media");
        assertThat(workload.project()).isEqualTo("default");
        assertThat(workload.location()).hasValue("node1");
    }

    @Test
    void mapsContainerToWorkload() {
        Container container =
                new Container("gitea", "gitea", VirtualMachineState.STOPPED, RESOURCES, Map.of(), Optional.empty());

        Workload workload = DomainMapper.toWorkload("default", container, METADATA);

        assertThat(workload.type()).isEqualTo(WorkloadType.CONTAINER);
        assertThat(workload.state()).isEqualTo(WorkloadState.STOPPED);
        assertThat(workload.location()).isEmpty();
    }

    @Test
    void mapsKnownStates() {
        assertThat(DomainMapper.toState(VirtualMachineState.RUNNING)).isEqualTo(WorkloadState.RUNNING);
        assertThat(DomainMapper.toState(VirtualMachineState.STOPPED)).isEqualTo(WorkloadState.STOPPED);
    }

    @Test
    void collapsesPausedSuspendedAndUnknownToUnknown() {
        assertThat(DomainMapper.toState(VirtualMachineState.PAUSED)).isEqualTo(WorkloadState.UNKNOWN);
        assertThat(DomainMapper.toState(VirtualMachineState.SUSPENDED)).isEqualTo(WorkloadState.UNKNOWN);
        assertThat(DomainMapper.toState(VirtualMachineState.UNKNOWN)).isEqualTo(WorkloadState.UNKNOWN);
    }
}
