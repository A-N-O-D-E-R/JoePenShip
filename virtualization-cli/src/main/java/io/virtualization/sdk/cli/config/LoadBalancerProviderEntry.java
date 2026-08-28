package io.virtualization.sdk.cli.config;

/** {@code virtualization.loadbalancers.providers.<name>} — its own config surface, not part of {@code virtualization.providers.*}. */
public record LoadBalancerProviderEntry(String type, String endpoint) {}
