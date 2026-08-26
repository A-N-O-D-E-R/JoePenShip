package io.virtualization.sdk.cli.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.virtualization.sdk.core.exception.ConfigurationException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Shared YAML plumbing for {@link DnsConfigLoader}/{@link CertificateConfigLoader} — same
 * kebab-case shape {@link ConfigLoader} uses for the main provider section, read from other
 * top-level sections of that same config file.
 */
final class ConfigYaml {

    static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory())
            .setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);

    private ConfigYaml() {}

    /** A missing node (no entries) if {@code configPath} doesn't exist yet — DNS/certificate config is optional, unlike the main provider file. */
    static JsonNode section(Path configPath, String... path) {
        JsonNode node = readRoot(configPath);
        for (String segment : path) {
            node = node.path(segment);
        }
        return node;
    }

    private static JsonNode readRoot(Path configPath) {
        if (!Files.isRegularFile(configPath)) {
            return MissingNode.getInstance();
        }
        try {
            return MAPPER.readTree(ConfigLoader.substituteEnvVars(Files.readString(configPath)));
        } catch (IOException e) {
            throw new ConfigurationException("Failed to read/parse configuration file: " + configPath, e);
        }
    }
}
