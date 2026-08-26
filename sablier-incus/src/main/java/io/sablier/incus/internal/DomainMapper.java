package io.sablier.incus.internal;

import io.sablier.core.Workload;
import io.sablier.core.WorkloadMetadata;
import io.sablier.core.WorkloadState;
import io.sablier.core.WorkloadType;
import io.virtualization.sdk.core.Container;
import io.virtualization.sdk.core.VirtualMachine;
import io.virtualization.sdk.core.VirtualMachineState;

/** Maps {@code virtualization-sdk}'s Incus domain records onto {@code sablier-core}'s provider-neutral domain records. */
public final class DomainMapper {

    private DomainMapper() {}

    public static Workload toWorkload(String project, VirtualMachine vm, WorkloadMetadata metadata) {
        return new Workload(
                vm.id(), vm.name(), WorkloadType.VIRTUAL_MACHINE, toState(vm.state()), metadata.group(), project, vm.location());
    }

    public static Workload toWorkload(String project, Container container, WorkloadMetadata metadata) {
        return new Workload(
                container.id(), container.name(), WorkloadType.CONTAINER, toState(container.state()), metadata.group(), project,
                container.location());
    }

    /**
     * {@code virtualization-core}'s {@link VirtualMachineState} has no equivalent of {@code
     * sablier-core}'s {@code STARTING}/{@code STOPPING}/{@code ERROR} transient states — those
     * collapse to {@code UNKNOWN} here. {@code READY} is never produced here either — it's a
     * Sablier-level outcome of a readiness check, not something the provider itself reports.
     */
    public static WorkloadState toState(VirtualMachineState state) {
        return switch (state) {
            case RUNNING -> WorkloadState.RUNNING;
            case STOPPED -> WorkloadState.STOPPED;
            case PAUSED, SUSPENDED, UNKNOWN -> WorkloadState.UNKNOWN;
        };
    }
}
