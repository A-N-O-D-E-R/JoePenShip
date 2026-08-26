package io.virtualization.sdk.incus.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * An Incus image as returned by {@code GET /1.0/images} (with {@code recursion=1}) or
 * {@code GET /1.0/images/{fingerprint}}. Unknown fields are ignored.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ImageDto(
        String fingerprint,
        String filename,
        long size,
        String architecture,
        boolean cached,
        @JsonProperty("public") boolean isPublic,
        String type, // "container" or "virtual-machine"
        @JsonProperty("created_at") String createdAt,
        @JsonProperty("uploaded_at") String uploadedAt,
        Map<String, String> properties, // e.g. "os" -> "Ubuntu", "release" -> "24.04"
        List<ImageAliasEntryDto> aliases) {}
