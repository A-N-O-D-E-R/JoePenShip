package io.virtualization.sdk.proxmox.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One row of {@code GET /cluster/resources?type=vm} — a QEMU VM or LXC container, wherever in the
 * cluster it lives. Unknown fields are ignored: Proxmox returns many more fields than the SDK maps.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ClusterResourceDto(
        String type, // "qemu" or "lxc"
        String node,
        Integer vmid,
        String name,
        String status, // "running", "stopped", ...
        Integer maxcpu,
        Long maxmem // bytes
) {

    public boolean isQemuVm() {
        return "qemu".equals(type);
    }
}
