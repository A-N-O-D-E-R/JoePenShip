package io.virtualization.sdk.cli.dns;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.dns.DnsProvider;
import io.virtualization.sdk.dns.DnsRecord;
import io.virtualization.sdk.dns.DnsRecordSpec;
import io.virtualization.sdk.dns.DnsZone;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * {@link DnsProvider} backed by a single JSON file, one per configured provider name — the CLI has
 * no long-lived process to keep an {@code InMemoryDnsProvider} alive between invocations, so
 * {@code dns record create} in one process and {@code dns record list} in the next need somewhere
 * durable to read from. Mirrors {@code JsonFileVpsRepository}'s pattern exactly.
 *
 * <p>Zones come entirely from {@code configZones} (config-file YAML), synthesized in memory on
 * every read the same way {@code InMemoryDnsProvider.addZone} does ({@code "mock-" + zoneName}) —
 * there's no zone-creation method on {@link DnsProvider} to persist one via, so only records ever
 * touch the file.
 *
 * <p>ponytail: whole-file read-modify-write under an in-process lock only, no OS file locking —
 * same accepted tradeoff as {@code JsonFileVpsRepository}.
 */
public final class JsonFileDnsProvider implements DnsProvider {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String name;
    private final List<String> configZones;
    private final Path file;

    public JsonFileDnsProvider(String name, List<String> configZones, Path file) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.configZones = List.copyOf(configZones);
        this.file = Objects.requireNonNull(file, "file must not be null");
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public List<DnsZone> zones() {
        return List.copyOf(zonesByName().values());
    }

    @Override
    public Optional<DnsZone> getZone(String domain) {
        return Optional.ofNullable(zonesByName().get(domain));
    }

    @Override
    public synchronized List<DnsRecord> records(String zone) {
        DnsProviderFile state = readFile();
        requireZone(zone);
        return List.copyOf(state.records().getOrDefault(zone, List.of()));
    }

    @Override
    public synchronized DnsRecord createRecord(String zone, DnsRecordSpec spec) {
        requireZone(zone);
        DnsProviderFile state = readFile();
        DnsRecord record = new DnsRecord(
                "rec-" + state.nextRecordId(), zone, spec.name(), spec.type(), spec.value(), spec.ttl(), spec.priority());
        Map<String, List<DnsRecord>> records = withZoneRecord(state, zone, zoneRecords -> zoneRecords.add(record));
        writeFile(new DnsProviderFile(records, state.nextRecordId() + 1));
        return record;
    }

    @Override
    public synchronized DnsRecord updateRecord(String zone, String recordId, DnsRecordSpec spec) {
        requireZone(zone);
        DnsProviderFile state = readFile();
        DnsRecord[] updated = new DnsRecord[1];
        Map<String, List<DnsRecord>> records = withZoneRecord(state, zone, zoneRecords -> {
            int index = indexOfRecord(zoneRecords, recordId, zone);
            updated[0] = new DnsRecord(recordId, zone, spec.name(), spec.type(), spec.value(), spec.ttl(), spec.priority());
            zoneRecords.set(index, updated[0]);
        });
        writeFile(new DnsProviderFile(records, state.nextRecordId()));
        return updated[0];
    }

    @Override
    public synchronized void deleteRecord(String zone, String recordId) {
        requireZone(zone);
        DnsProviderFile state = readFile();
        Map<String, List<DnsRecord>> records = withZoneRecord(state, zone, zoneRecords ->
                zoneRecords.remove(indexOfRecord(zoneRecords, recordId, zone)));
        writeFile(new DnsProviderFile(records, state.nextRecordId()));
    }

    private Map<String, DnsZone> zonesByName() {
        Map<String, DnsZone> zones = new LinkedHashMap<>();
        for (String zoneName : configZones) {
            zones.put(zoneName, new DnsZone(zoneName, name, "mock-" + zoneName));
        }
        return zones;
    }

    private void requireZone(String zone) {
        if (!zonesByName().containsKey(zone)) {
            throw new ResourceNotFoundException("No DNS zone '" + zone + "' on provider '" + name + "'");
        }
    }

    private Map<String, List<DnsRecord>> withZoneRecord(DnsProviderFile state, String zone, java.util.function.Consumer<List<DnsRecord>> mutation) {
        Map<String, List<DnsRecord>> records = new LinkedHashMap<>(state.records());
        List<DnsRecord> zoneRecords = new ArrayList<>(records.getOrDefault(zone, List.of()));
        mutation.accept(zoneRecords);
        records.put(zone, zoneRecords);
        return records;
    }

    private static int indexOfRecord(List<DnsRecord> zoneRecords, String recordId, String zone) {
        for (int i = 0; i < zoneRecords.size(); i++) {
            if (zoneRecords.get(i).id().equals(recordId)) {
                return i;
            }
        }
        throw new ResourceNotFoundException("No DNS record '" + recordId + "' in zone '" + zone + "'");
    }

    private DnsProviderFile readFile() {
        if (!Files.exists(file)) {
            return new DnsProviderFile(Map.of(), 1);
        }
        try {
            return MAPPER.readValue(file.toFile(), DnsProviderFile.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read DNS state file '" + file + "'", e);
        }
    }

    private void writeFile(DnsProviderFile state) {
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            MAPPER.writeValue(file.toFile(), state);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write DNS state file '" + file + "'", e);
        }
    }

    private record DnsProviderFile(Map<String, List<DnsRecord>> records, int nextRecordId) {}
}
