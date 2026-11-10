package io.virtualization.sdk.spring.iam;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds:
 * <pre>{@code
 * virtualization:
 *   iam:
 *     enabled: true
 *     state-file: /custom/path/iam.json   # optional, default ${user.home}/.virtualization/iam.json
 * }</pre>
 * Off by default — same opt-in stance {@code virtualization.domains.enabled}/{@code
 * virtualization.certificates.enabled} take.
 */
@ConfigurationProperties(prefix = "virtualization.iam")
public record IamProperties(boolean enabled, String stateFile) {}
