package io.virtualization.sdk.cli.config;

import com.fasterxml.jackson.databind.JsonNode;
import io.virtualization.sdk.cli.dns.JsonFileDnsProvider;
import io.virtualization.sdk.core.exception.ConfigurationException;
import io.virtualization.sdk.dns.DnsProvider;
import io.virtualization.sdk.dns.DnsProviderRegistry;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Loads {@code virtualization.dns.providers.<name>} — same shape as the Spring Boot starter's
 * {@code DnsAutoConfiguration}, minus its bean-gating flag (Picocli subcommands register
 * unconditionally). Only {@code type: mock} is accepted, backed by a {@link JsonFileDnsProvider} so
 * records survive across CLI invocations, one JSON file per provider name.
 */
public final class DnsConfigLoader {

    private DnsConfigLoader() {}

    public static DnsProviderRegistry loadRegistry(Path configPath, Function<String, Path> stateFileForProvider) {
        Map<String, DnsProvider> providers = new LinkedHashMap<>();
        JsonNode section = ConfigYaml.section(configPath, "virtualization", "dns", "providers");
        section.fields().forEachRemaining(field -> {
            String name = field.getKey();
            DnsProviderEntry entry = ConfigYaml.MAPPER.convertValue(field.getValue(), DnsProviderEntry.class);
            providers.put(name, createDnsProvider(name, entry, stateFileForProvider.apply(name)));
        });
        return new DnsProviderRegistry(providers);
    }

    private static DnsProvider createDnsProvider(String name, DnsProviderEntry entry, Path stateFile) {
        if (entry.type() == null || entry.type().isBlank()) {
            throw new ConfigurationException("DNS provider '" + name + "' is missing required field 'type'.");
        }
        if (!"mock".equals(entry.type())) {
            throw new ConfigurationException("DNS provider '" + name + "' has unknown type '" + entry.type() + "'");
        }
        List<String> zones = entry.zones() != null ? entry.zones() : List.of();
        return new JsonFileDnsProvider(name, zones, stateFile);
    }
}
