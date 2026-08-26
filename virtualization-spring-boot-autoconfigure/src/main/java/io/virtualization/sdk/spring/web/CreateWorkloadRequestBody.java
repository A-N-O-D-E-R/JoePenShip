package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.core.image.WorkloadType;

import java.util.List;
import java.util.Map;

/** {@code POST /api/v1/workloads} request body. */
record CreateWorkloadRequestBody(
        String provider,
        String name,
        WorkloadType type,
        ImageRequestBody image,
        Integer cpu,
        Long memoryMb,
        List<String> storage,
        List<String> networks,
        Map<String, String> environment,
        Map<String, Object> providerOptions) {

    record ImageRequestBody(String remote, String name) {}
}
