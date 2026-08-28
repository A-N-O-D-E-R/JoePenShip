package io.virtualization.sdk.spring.web;

public record CreateSubnetRequestBody(String cidr, String gateway) {}
