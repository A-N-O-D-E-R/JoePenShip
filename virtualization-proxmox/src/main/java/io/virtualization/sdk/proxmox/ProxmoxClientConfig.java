package io.virtualization.sdk.proxmox;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

/**
 * Connection settings for a {@link ProxmoxProvider}.
 *
 * @param endpoint         base URL of the Proxmox VE API, e.g. {@code https://pve.example.com:8006}
 * @param credentials      API token credentials
 * @param verifySsl        when {@code false}, TLS certificate validation is disabled (self-signed
 *                         Proxmox installs) — off by default risk, must be opted into explicitly
 * @param connectTimeout   TCP connect timeout
 * @param requestTimeout   per-request timeout
 * @param taskPollInterval how often to poll a Proxmox task's status while awaiting an {@link
 *                         io.virtualization.sdk.core.Operation}
 */
public record ProxmoxClientConfig(
        URI endpoint,
        ProxmoxCredentials credentials,
        boolean verifySsl,
        Duration connectTimeout,
        Duration requestTimeout,
        Duration taskPollInterval) {

    public ProxmoxClientConfig {
        Objects.requireNonNull(endpoint, "endpoint must not be null");
        Objects.requireNonNull(credentials, "credentials must not be null");
        Objects.requireNonNull(connectTimeout, "connectTimeout must not be null");
        Objects.requireNonNull(requestTimeout, "requestTimeout must not be null");
        Objects.requireNonNull(taskPollInterval, "taskPollInterval must not be null");
        if (connectTimeout.isNegative() || connectTimeout.isZero()) {
            throw new IllegalArgumentException("connectTimeout must be positive");
        }
        if (requestTimeout.isNegative() || requestTimeout.isZero()) {
            throw new IllegalArgumentException("requestTimeout must be positive");
        }
        if (taskPollInterval.isNegative() || taskPollInterval.isZero()) {
            throw new IllegalArgumentException("taskPollInterval must be positive");
        }
    }

    /** Config with TLS verification enabled and sensible default timeouts. */
    public static ProxmoxClientConfig of(URI endpoint, ProxmoxCredentials credentials) {
        return new ProxmoxClientConfig(
                endpoint, credentials, true, Duration.ofSeconds(10), Duration.ofSeconds(30), Duration.ofSeconds(1));
    }
}
