package io.virtualization.sdk.incus.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** {@code GET /1.0/images/aliases/{name}} — resolves an alias to the fingerprint it targets. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ImageAliasDto(String name, String target, String description, String type) {}
