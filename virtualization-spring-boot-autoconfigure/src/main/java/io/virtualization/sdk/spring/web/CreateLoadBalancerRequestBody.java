package io.virtualization.sdk.spring.web;

import java.util.List;

public record CreateLoadBalancerRequestBody(String name, List<String> listenAddresses) {}
