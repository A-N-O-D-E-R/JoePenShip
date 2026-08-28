package io.virtualization.sdk.incus.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** {@code GET /1.0/storage-pools/{pool}/volumes/custom/{name}/snapshots} (with {@code recursion=1}). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StorageVolumeSnapshotDto(String name, @JsonProperty("created_at") String createdAt) {}
