package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.storage.VolumeContentType;

import java.util.Map;

public record CreateVolumeRequestBody(String name, VolumeContentType contentType, String size, Map<String, String> config) {}
