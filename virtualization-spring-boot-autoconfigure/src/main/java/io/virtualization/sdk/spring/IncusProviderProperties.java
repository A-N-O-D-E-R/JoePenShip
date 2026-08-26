package io.virtualization.sdk.spring;

import static io.virtualization.sdk.spring.internal.RequiredField.require;

/** Validated view of a {@code type: incus} {@link ProviderProperties} entry. */
public record IncusProviderProperties(String endpoint, String clientCertPath, String clientKeyPath, boolean verifySsl) {

    static IncusProviderProperties from(String providerName, ProviderProperties properties) {
        return new IncusProviderProperties(
                require(providerName, "incus", properties.endpoint(), "endpoint"),
                require(providerName, "incus", properties.clientCertPath(), "client-cert-path"),
                require(providerName, "incus", properties.clientKeyPath(), "client-key-path"),
                properties.verifySsl());
    }
}
