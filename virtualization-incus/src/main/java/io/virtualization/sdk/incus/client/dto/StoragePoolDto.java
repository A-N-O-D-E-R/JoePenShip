package io.virtualization.sdk.incus.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/** {@code GET /1.0/storage-pools} (with {@code recursion=1}) or {@code GET /1.0/storage-pools/{name}}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StoragePoolDto(String name, String driver, String description, Map<String, String> config, String status) {}
