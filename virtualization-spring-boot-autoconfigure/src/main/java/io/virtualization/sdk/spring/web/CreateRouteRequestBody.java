package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.loadbalancer.Backend;
import io.virtualization.sdk.loadbalancer.HealthCheck;

import java.util.List;

public record CreateRouteRequestBody(String host, List<Backend> backends, HealthCheck healthCheck) {}
