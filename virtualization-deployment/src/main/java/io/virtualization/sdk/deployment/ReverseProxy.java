package io.virtualization.sdk.deployment;

/** Which reverse proxy to reload after deploying new certificate material. {@code NONE} skips reload entirely. */
public enum ReverseProxy {
    NGINX,
    APACHE,
    CADDY,
    TRAEFIK,
    NONE
}
