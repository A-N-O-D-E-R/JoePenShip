package io.virtualization.sdk.qemu.internal;

import com.fasterxml.jackson.databind.JsonNode;
import io.virtualization.sdk.core.ComputeResources;
import io.virtualization.sdk.core.VirtualMachine;
import io.virtualization.sdk.core.VirtualMachineState;

/** Maps QMP command responses onto {@code virtualization-core} domain records. */
public final class DomainMapper {

    private DomainMapper() {}

    /**
     * @param statusReturn the {@code return} payload of {@code query-status}
     * @param cpusReturn   the {@code return} payload of {@code query-cpus} (an array)
     */
    public static VirtualMachine toVirtualMachine(String vmId, JsonNode statusReturn, JsonNode cpusReturn) {
        VirtualMachineState state = toState(statusReturn.path("status").asText(null));
        int cpuCores = cpusReturn.isArray() && cpusReturn.size() > 0 ? cpusReturn.size() : 1;
        // QEMU's base QMP command set (query-status, query-cpus, ...) doesn't expose configured
        // memory size; query-memory-size-summary would, but that's out of this iteration's scope.
        long memoryMb = 0;
        return new VirtualMachine(vmId, vmId, state, new ComputeResources(cpuCores, memoryMb));
    }

    private static VirtualMachineState toState(String status) {
        if (status == null) {
            return VirtualMachineState.UNKNOWN;
        }
        return switch (status) {
            case "running" -> VirtualMachineState.RUNNING;
            case "paused" -> VirtualMachineState.PAUSED;
            case "shutdown" -> VirtualMachineState.STOPPED;
            case "suspended" -> VirtualMachineState.SUSPENDED;
            default -> VirtualMachineState.UNKNOWN;
        };
    }
}
