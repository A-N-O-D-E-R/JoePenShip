package io.virtualization.sdk.spring.iam.web;

/** @param ttlSeconds {@code null} = never expires */
public record IssueApiCredentialRequestBody(String ownerId, String name, Long ttlSeconds) {}
