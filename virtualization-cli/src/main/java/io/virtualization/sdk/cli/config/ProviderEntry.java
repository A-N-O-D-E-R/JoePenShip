package io.virtualization.sdk.cli.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * One {@code virtualization.providers.<name>} entry from the CLI's YAML configuration. Field
 * names bind from kebab-case YAML keys (e.g. {@code token-secret} -> {@link #tokenSecret()}); not
 * every field applies to every provider {@link #type()} — see {@link ProviderFactory}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProviderEntry(
        String type,
        String endpoint,
        String tokenId,
        String tokenSecret,
        Boolean verifySsl,
        String clientCertPath,
        String clientKeyPath,
        String socket,
        String host,
        Integer port) {}
