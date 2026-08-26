package io.virtualization.sdk.cli.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.virtualization.sdk.core.ProviderRegistry;
import io.virtualization.sdk.core.VirtualizationClient;
import io.virtualization.sdk.core.VirtualizationProvider;
import io.virtualization.sdk.core.exception.ConfigurationException;
import io.virtualization.sdk.core.image.ImageProvider;
import io.virtualization.sdk.core.image.ImageProviderRegistry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads the CLI's {@code virtualization.providers.<name>} YAML configuration — the same shape
 * documented for the Spring Boot starter (section 13 of the SDK spec) — and builds a {@link
 * VirtualizationClient} from it.
 */
public final class ConfigLoader {

    private static final Pattern ENV_VAR_REFERENCE = Pattern.compile("\\$\\{([A-Za-z_][A-Za-z0-9_]*)}");

    private ConfigLoader() {}

    public static VirtualizationClient loadClient(Path configPath) {
        if (!Files.isRegularFile(configPath)) {
            throw new ConfigurationException("Provider configuration file not found: " + configPath);
        }
        String yaml;
        try {
            yaml = Files.readString(configPath);
        } catch (IOException e) {
            throw new ConfigurationException("Failed to read provider configuration file: " + configPath, e);
        }
        return loadClientFromYaml(yaml);
    }

    static VirtualizationClient loadClientFromYaml(String yaml) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.KEBAB_CASE);

        JsonNode root;
        try {
            root = mapper.readTree(substituteEnvVars(yaml));
        } catch (IOException e) {
            throw new ConfigurationException("Failed to parse provider configuration YAML", e);
        }

        Map<String, VirtualizationProvider> providers = new LinkedHashMap<>();
        Map<String, ImageProvider> imageProviders = new LinkedHashMap<>();
        root.path("virtualization").path("providers").fields().forEachRemaining(field -> {
            String name = field.getKey();
            ProviderEntry entry;
            try {
                entry = mapper.convertValue(field.getValue(), ProviderEntry.class);
            } catch (IllegalArgumentException e) {
                throw new ConfigurationException("Provider '" + name + "' has an invalid configuration", e);
            }
            providers.put(name, ProviderFactory.create(name, entry));
            ProviderFactory.createImageProvider(name, entry).ifPresent(imageProvider -> imageProviders.put(name, imageProvider));
        });
        return new VirtualizationClient(new ProviderRegistry(providers), new ImageProviderRegistry(imageProviders));
    }

    static String substituteEnvVars(String yaml) {
        Matcher matcher = ENV_VAR_REFERENCE.matcher(yaml);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group(1);
            String value = System.getenv(name);
            if (value == null) {
                throw new ConfigurationException("Environment variable '" + name + "' referenced in configuration is not set");
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}
