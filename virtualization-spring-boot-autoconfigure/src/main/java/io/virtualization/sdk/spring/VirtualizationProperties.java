package io.virtualization.sdk.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Binds:
 * <pre>{@code
 * virtualization:
 *   providers:
 *     production:
 *       type: proxmox
 *       endpoint: https://pve.example.com:8006
 *       token-id: root@pam!sdk
 *       token-secret: ${PROXMOX_TOKEN_SECRET}
 * }</pre>
 */
@ConfigurationProperties(prefix = "virtualization")
public record VirtualizationProperties(Map<String, ProviderProperties> providers) {

    public VirtualizationProperties {
        providers = providers != null ? Map.copyOf(providers) : Map.of();
    }
}
