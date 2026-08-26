package io.virtualization.sdk.deployment;

/**
 * Where a certificate gets deployed. {@link VpsDeploymentTarget} is the only implementation so
 * far — {@code ReverseProxy}/{@code LoadBalancer}/{@code Ingress}-shaped targets (distinct from
 * the {@link ReverseProxy} enum, which just names which service a VPS-hosted deployment reloads)
 * are future options, not implemented yet. A marker interface, not sealed, so new target types
 * don't require editing this file.
 */
public interface DeploymentTarget {}
