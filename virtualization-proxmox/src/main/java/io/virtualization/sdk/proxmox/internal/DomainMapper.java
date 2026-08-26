package io.virtualization.sdk.proxmox.internal;

import io.virtualization.sdk.core.ComputeResources;
import io.virtualization.sdk.core.VirtualMachine;
import io.virtualization.sdk.core.VirtualMachineState;
import io.virtualization.sdk.proxmox.client.dto.ClusterResourceDto;

/** Maps Proxmox API DTOs onto {@code virtualization-core} domain records. */
public final class DomainMapper {

    private DomainMapper() {}

    public static VirtualMachine toVirtualMachine(ClusterResourceDto dto) {
        String id = String.valueOf(dto.vmid());
        String name = dto.name() != null ? dto.name() : "vm-" + dto.vmid();
        VirtualMachineState state = toState(dto.status());
        ComputeResources resources = new ComputeResources(
                dto.maxcpu() != null && dto.maxcpu() > 0 ? dto.maxcpu() : 1,
                dto.maxmem() != null ? dto.maxmem() / (1024 * 1024) : 0);
        return new VirtualMachine(id, name, state, resources);
    }

    private static VirtualMachineState toState(String status) {
        if (status == null) {
            return VirtualMachineState.UNKNOWN;
        }
        return switch (status) {
            case "running" -> VirtualMachineState.RUNNING;
            case "stopped" -> VirtualMachineState.STOPPED;
            case "paused" -> VirtualMachineState.PAUSED;
            case "suspended" -> VirtualMachineState.SUSPENDED;
            default -> VirtualMachineState.UNKNOWN;
        };
    }
}
