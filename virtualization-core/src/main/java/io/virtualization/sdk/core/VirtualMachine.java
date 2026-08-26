package io.virtualization.sdk.core;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** A provider-neutral view of a virtual machine. */
public record VirtualMachine(
        String id,
        String name,
        VirtualMachineState state,
        ComputeResources resources,
        Map<String, String> metadata,
        Optional<String> location) {

    public VirtualMachine {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(resources, "resources must not be null");
        Objects.requireNonNull(location, "location must not be null (use Optional.empty())");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
    }

    public VirtualMachine(String id, String name, VirtualMachineState state, ComputeResources resources) {
        this(id, name, state, resources, Map.of(), Optional.empty());
    }
}
