package io.virtualization.sdk.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds:
 * <pre>{@code
 * virtualization:
 *   vps:
 *     provider: production
 * }</pre>
 * {@code provider} must name an entry already configured under {@code virtualization.providers}
 * — the single backend the {@link io.virtualization.sdk.vps.VpsManager} bean provisions against.
 * {@code virtualization-vps} has no notion of routing a single VPS across multiple providers yet.
 */
@ConfigurationProperties(prefix = "virtualization.vps")
public record VpsProperties(String provider) {}
