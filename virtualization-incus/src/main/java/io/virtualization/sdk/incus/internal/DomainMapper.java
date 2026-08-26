package io.virtualization.sdk.incus.internal;

import io.virtualization.sdk.core.ComputeResources;
import io.virtualization.sdk.core.Container;
import io.virtualization.sdk.core.VirtualMachine;
import io.virtualization.sdk.core.VirtualMachineState;
import io.virtualization.sdk.incus.client.dto.InstanceDto;

import java.util.Map;
import java.util.Optional;

/** Maps Incus API DTOs onto {@code virtualization-core} domain records. */
public final class DomainMapper {

    private static final int DEFAULT_CPU_CORES = 1;
    private static final long DEFAULT_MEMORY_MB = 512;

    private DomainMapper() {}

    public static VirtualMachine toVirtualMachine(InstanceDto dto) {
        Map<String, String> config = config(dto);
        return new VirtualMachine(dto.name(), dto.name(), toState(dto.status()), resources(config), config, location(dto));
    }

    public static Container toContainer(InstanceDto dto) {
        Map<String, String> config = config(dto);
        return new Container(dto.name(), dto.name(), toState(dto.status()), resources(config), config, location(dto));
    }

    private static Map<String, String> config(InstanceDto dto) {
        return dto.config() != null ? dto.config() : Map.of();
    }

    private static Optional<String> location(InstanceDto dto) {
        return dto.location() == null || dto.location().isBlank() ? Optional.empty() : Optional.of(dto.location());
    }

    private static ComputeResources resources(Map<String, String> config) {
        return new ComputeResources(
                parseCpuCores(config.get("limits.cpu")),
                MemorySizeParser.toMegabytes(config.get("limits.memory"), DEFAULT_MEMORY_MB));
    }

    private static int parseCpuCores(String limitsCpu) {
        if (limitsCpu == null || limitsCpu.isBlank()) {
            return DEFAULT_CPU_CORES;
        }
        try {
            return Integer.parseInt(limitsCpu.trim());
        } catch (NumberFormatException e) {
            // e.g. a pinned core list like "0-1,3" rather than a plain count — not parsed further
            return DEFAULT_CPU_CORES;
        }
    }

    private static VirtualMachineState toState(String status) {
        if (status == null) {
            return VirtualMachineState.UNKNOWN;
        }
        return switch (status) {
            case "Running" -> VirtualMachineState.RUNNING;
            case "Stopped" -> VirtualMachineState.STOPPED;
            case "Frozen" -> VirtualMachineState.PAUSED;
            default -> VirtualMachineState.UNKNOWN;
        };
    }
}
