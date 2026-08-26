package io.virtualization.sdk.incus.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/** {@code GET /1.0/operations/{id}/wait} — the state of a background Incus operation. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OperationStatusDto(
        String status, // "Running", "Success", "Failure", "Cancelled"
        String err, // populated on Failure
        Map<String, Object> metadata // operation-type-specific detail, e.g. "fingerprint" or "download_progress"
) {

    public boolean isFinished() {
        return "Success".equals(status) || "Failure".equals(status) || "Cancelled".equals(status);
    }

    public boolean isSuccessful() {
        return "Success".equals(status);
    }

    /** The fingerprint of the image an image-create operation (pull or import) produced, once successful. */
    public String fingerprint() {
        return metadata != null ? (String) metadata.get("fingerprint") : null;
    }

    /** Incus's human-readable pull progress, e.g. {@code "42% (10.5MB/25.0MB)"}, while a pull is running. */
    public String downloadProgress() {
        return metadata != null ? (String) metadata.get("download_progress") : null;
    }
}
