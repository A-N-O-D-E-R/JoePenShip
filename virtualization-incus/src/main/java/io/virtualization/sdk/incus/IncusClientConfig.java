package io.virtualization.sdk.incus;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

/**
 * Connection settings for an {@link IncusProvider}.
 *
 * @param endpoint            base URL of the Incus API, e.g. {@code https://incus.example.com:8443}
 * @param credentials         mutual-TLS client certificate credentials
 * @param project             the Incus project this provider is scoped to. A deployment spanning
 *                            multiple projects registers one {@link IncusProvider} per project.
 * @param verifySsl           when {@code false}, server TLS certificate validation is disabled
 *                            (self-signed Incus installs) — must be opted into explicitly
 * @param connectTimeout      TCP connect timeout
 * @param requestTimeout      per-request timeout
 * @param operationWaitTimeout per-call timeout passed to Incus's long-polling
 *                            {@code /operations/{id}/wait} endpoint while awaiting an {@link
 *                            io.virtualization.sdk.core.Operation}
 */
public record IncusClientConfig(
        URI endpoint,
        IncusTlsCredentials credentials,
        String project,
        boolean verifySsl,
        Duration connectTimeout,
        Duration requestTimeout,
        Duration operationWaitTimeout) {

    public IncusClientConfig {
        Objects.requireNonNull(endpoint, "endpoint must not be null");
        Objects.requireNonNull(credentials, "credentials must not be null");
        Objects.requireNonNull(project, "project must not be null");
        Objects.requireNonNull(connectTimeout, "connectTimeout must not be null");
        Objects.requireNonNull(requestTimeout, "requestTimeout must not be null");
        Objects.requireNonNull(operationWaitTimeout, "operationWaitTimeout must not be null");
        if (project.isBlank()) {
            throw new IllegalArgumentException("project must not be blank");
        }
        if (connectTimeout.isNegative() || connectTimeout.isZero()) {
            throw new IllegalArgumentException("connectTimeout must be positive");
        }
        if (requestTimeout.isNegative() || requestTimeout.isZero()) {
            throw new IllegalArgumentException("requestTimeout must be positive");
        }
        if (operationWaitTimeout.isNegative() || operationWaitTimeout.isZero()) {
            throw new IllegalArgumentException("operationWaitTimeout must be positive");
        }
    }

    /** Config for the {@code "default"} project, with TLS verification enabled and sensible default timeouts. */
    public static IncusClientConfig of(URI endpoint, IncusTlsCredentials credentials) {
        return new IncusClientConfig(
                endpoint, credentials, "default", true, Duration.ofSeconds(10), Duration.ofSeconds(30), Duration.ofSeconds(5));
    }
}
