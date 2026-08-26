package io.virtualization.sdk.spring;

import static io.virtualization.sdk.spring.internal.RequiredField.require;

/** Validated view of a {@code type: proxmox} {@link ProviderProperties} entry. */
public record ProxmoxProviderProperties(String endpoint, String tokenId, String tokenSecret, boolean verifySsl) {

    static ProxmoxProviderProperties from(String providerName, ProviderProperties properties) {
        return new ProxmoxProviderProperties(
                require(providerName, "proxmox", properties.endpoint(), "endpoint"),
                require(providerName, "proxmox", properties.tokenId(), "token-id"),
                require(providerName, "proxmox", properties.tokenSecret(), "token-secret"),
                properties.verifySsl());
    }
}
