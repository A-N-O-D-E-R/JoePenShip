package io.virtualization.sdk.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds:
 * <pre>{@code
 * virtualization:
 *   domains:
 *     enabled: true
 * }</pre>
 * Off by default — domain management is an opt-in subsystem, same stance {@code
 * virtualization.vps.provider} takes for the VPS layer.
 */
@ConfigurationProperties(prefix = "virtualization.domains")
public record DomainProperties(boolean enabled) {}
