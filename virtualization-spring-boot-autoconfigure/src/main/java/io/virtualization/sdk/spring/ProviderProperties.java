package io.virtualization.sdk.spring;

import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * One {@code virtualization.providers.<name>} entry. Flat by necessity — Spring's configuration
 * binder has no built-in support for a type-discriminated union, so every provider type's fields
 * live side by side here; not every field applies to every {@link #type()}. See {@link
 * ProxmoxProviderProperties}, {@link IncusProviderProperties} and {@link QemuProviderProperties}
 * for the validated, type-specific view each provider is actually built from.
 */
public record ProviderProperties(
        String type,
        String endpoint,
        String tokenId,
        String tokenSecret,
        @DefaultValue("true") boolean verifySsl,
        String clientCertPath,
        String clientKeyPath,
        String socket,
        String host,
        Integer port) {}
