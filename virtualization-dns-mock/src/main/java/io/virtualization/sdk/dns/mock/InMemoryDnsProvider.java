package io.virtualization.sdk.dns.mock;

import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.dns.DnsProvider;
import io.virtualization.sdk.dns.DnsRecord;
import io.virtualization.sdk.dns.DnsRecordSpec;
import io.virtualization.sdk.dns.DnsZone;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory {@link DnsProvider} — zones must be registered explicitly via {@link #addZone}, since
 * {@code DnsProvider} itself has no zone-creation method (real backends manage zone creation
 * outside this SDK; Version 1 only manages DNS for zones a caller already owns).
 *
 * <p>ponytail: one lock per instance (record mutation methods are {@code synchronized}), not
 * per-zone — fine for local dev/testing, this isn't meant for high-throughput production use.
 */
public final class InMemoryDnsProvider implements DnsProvider {

    private final String name;
    private final Map<String, DnsZone> zones = new ConcurrentHashMap<>();
    private final Map<String, List<DnsRecord>> records = new ConcurrentHashMap<>();
    private final AtomicInteger recordIdCounter = new AtomicInteger();

    public InMemoryDnsProvider(String name) {
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    /** Registers a new zone this provider serves records for. */
    public synchronized DnsZone addZone(String zoneName) {
        Objects.requireNonNull(zoneName, "zoneName must not be null");
        DnsZone zone = new DnsZone(zoneName, name, "mock-" + zoneName);
        zones.put(zoneName, zone);
        records.putIfAbsent(zoneName, new ArrayList<>());
        return zone;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public List<DnsZone> zones() {
        return List.copyOf(zones.values());
    }

    @Override
    public Optional<DnsZone> getZone(String domain) {
        return Optional.ofNullable(zones.get(domain));
    }

    @Override
    public synchronized List<DnsRecord> records(String zone) {
        return List.copyOf(requireZoneRecords(zone));
    }

    @Override
    public synchronized DnsRecord createRecord(String zone, DnsRecordSpec spec) {
        List<DnsRecord> zoneRecords = requireZoneRecords(zone);
        DnsRecord record = new DnsRecord(
                "rec-" + recordIdCounter.incrementAndGet(), zone, spec.name(), spec.type(), spec.value(), spec.ttl(),
                spec.priority());
        zoneRecords.add(record);
        return record;
    }

    @Override
    public synchronized DnsRecord updateRecord(String zone, String recordId, DnsRecordSpec spec) {
        List<DnsRecord> zoneRecords = requireZoneRecords(zone);
        int index = indexOfRecord(zoneRecords, recordId, zone);
        DnsRecord updated = new DnsRecord(recordId, zone, spec.name(), spec.type(), spec.value(), spec.ttl(), spec.priority());
        zoneRecords.set(index, updated);
        return updated;
    }

    @Override
    public synchronized void deleteRecord(String zone, String recordId) {
        List<DnsRecord> zoneRecords = requireZoneRecords(zone);
        zoneRecords.remove(indexOfRecord(zoneRecords, recordId, zone));
    }

    private List<DnsRecord> requireZoneRecords(String zone) {
        List<DnsRecord> zoneRecords = records.get(zone);
        if (zoneRecords == null) {
            throw new ResourceNotFoundException("No DNS zone '" + zone + "' on provider '" + name + "'");
        }
        return zoneRecords;
    }

    private static int indexOfRecord(List<DnsRecord> zoneRecords, String recordId, String zone) {
        for (int i = 0; i < zoneRecords.size(); i++) {
            if (zoneRecords.get(i).id().equals(recordId)) {
                return i;
            }
        }
        throw new ResourceNotFoundException("No DNS record '" + recordId + "' in zone '" + zone + "'");
    }
}
