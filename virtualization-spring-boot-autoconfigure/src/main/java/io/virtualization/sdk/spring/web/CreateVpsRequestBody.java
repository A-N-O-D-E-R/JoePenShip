package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.vps.VpsType;

import java.util.List;
import java.util.Map;

/** {@code POST /api/v1/vps} request body. */
record CreateVpsRequestBody(
        String name,
        VpsType type,
        ImageRequestBody image,
        Integer cpu,
        Long memoryMb,
        Long diskMb,
        String storagePool,
        String volumeType,
        NetworkRequestBody network,
        List<String> sshPublicKeys,
        String cloudInit,
        Map<String, String> metadata,
        Map<String, String> labels,
        String location,
        String project,
        String idempotencyKey) {

    record ImageRequestBody(String provider, String remote, String name) {}

    record NetworkRequestBody(String network, String ipv4, String ipv6, String hostname) {}
}
