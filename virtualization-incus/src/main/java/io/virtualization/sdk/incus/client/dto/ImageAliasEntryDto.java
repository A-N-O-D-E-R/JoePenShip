package io.virtualization.sdk.incus.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** An alias entry embedded in {@link ImageDto#aliases()}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ImageAliasEntryDto(String name, String description) {}
