package io.virtualization.sdk.incus.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * {@code GET /1.0/storage-pools/{pool}/volumes} (with {@code recursion=1}) or {@code GET
 * .../volumes/{type}/{name}}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StorageVolumeDto(
        String name,
        String type,
        @JsonProperty("content_type") String contentType,
        Map<String, String> config) {}
