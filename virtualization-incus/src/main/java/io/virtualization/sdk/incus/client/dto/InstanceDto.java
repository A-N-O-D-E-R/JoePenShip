package io.virtualization.sdk.incus.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * An Incus instance (container or virtual machine) as returned by {@code GET /1.0/instances}
 * (with {@code recursion=1}) or {@code GET /1.0/instances/{name}}. Unknown fields are ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record InstanceDto(String name, String type, String status, String location, Map<String, String> config) {

    public boolean isVirtualMachine() {
        return "virtual-machine".equals(type);
    }

    public boolean isContainer() {
        return "container".equals(type);
    }
}
