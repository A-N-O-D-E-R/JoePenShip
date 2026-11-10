package io.virtualization.sdk.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds:
 * <pre>{@code
 * virtualization:
 *   network:
 *     state-file: /custom/path/network.json   # optional, default ${user.home}/.virtualization/network.json
 * }</pre>
 * No {@code enabled} flag — {@link NetworkAutoConfiguration} always registers, the same stance
 * {@link ProjectAutoConfiguration} takes.
 */
@ConfigurationProperties(prefix = "virtualization.network")
public record NetworkProperties(String stateFile) {}
