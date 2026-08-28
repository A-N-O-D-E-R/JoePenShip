package io.virtualization.sdk.cli.config;

import com.fasterxml.jackson.databind.JsonNode;
import io.virtualization.sdk.storage.StorageProvider;
import io.virtualization.sdk.storage.StorageProviderRegistry;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads a {@link StorageProviderRegistry} from the **same** {@code virtualization.providers.<name>}
 * YAML section {@link ConfigLoader} reads for {@link io.virtualization.sdk.core.ProviderRegistry}/
 * {@link io.virtualization.sdk.core.image.ImageProviderRegistry} — a storage-capable provider (only
 * {@code incus} today) is the same physical connection, not a separate config surface, so this
 * re-parses that one section independently rather than duplicating it under a new key. Mirrors
 * {@link DnsConfigLoader}'s "each config concern gets its own small loader" shape.
 */
public final class StorageConfigLoader {

    private StorageConfigLoader() {}

    public static StorageProviderRegistry loadRegistry(Path configPath) {
        Map<String, StorageProvider> providers = new LinkedHashMap<>();
        JsonNode section = ConfigYaml.section(configPath, "virtualization", "providers");
        section.fields().forEachRemaining(field -> {
            String name = field.getKey();
            ProviderEntry entry = ConfigYaml.MAPPER.convertValue(field.getValue(), ProviderEntry.class);
            ProviderFactory.createStorageProvider(name, entry).ifPresent(provider -> providers.put(name, provider));
        });
        return new StorageProviderRegistry(providers);
    }
}
