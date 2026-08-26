package io.virtualization.sdk.spring.web;

/** {@code POST /api/v1/vps/{id}/rebuild} request body. */
record RebuildVpsRequestBody(CreateVpsRequestBody.ImageRequestBody image) {}
