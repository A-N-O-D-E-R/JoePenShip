package io.virtualization.sdk.proxmox.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** {@code GET /nodes/{node}/tasks/{upid}/status} — the state of an asynchronous Proxmox task. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskStatusDto(
        String status, // "running" or "stopped"
        String exitstatus // "OK" on success, an error message on failure, null while running
) {

    public boolean isFinished() {
        return "stopped".equals(status);
    }

    public boolean isSuccessful() {
        return "OK".equals(exitstatus);
    }
}
