package io.virtualization.sdk.provisioning.support;

import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.dns.DnsProvider;
import io.virtualization.sdk.dns.DnsRecord;
import io.virtualization.sdk.dns.DnsRecordSpec;
import io.virtualization.sdk.dns.DnsZone;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/** Hand-written test double for {@link DnsProvider}, independently duplicated per this repo's "no test-jar sharing" precedent. */
public final class FakeDnsProvider implements DnsProvider {

    private final String name;
    private final Map<String, DnsZone> zones = new LinkedHashMap<>();
    private final Map<String, List<DnsRecord>> records = new LinkedHashMap<>();
    private final List<String> calls = new ArrayList<>();
    private final AtomicInteger recordIdCounter = new AtomicInteger();

    private FakeDnsProvider(String name) {
        this.name = name;
    }

    public static FakeDnsProvider named(String name) {
        return new FakeDnsProvider(name);
    }

    public FakeDnsProvider withZone(DnsZone zone) {
        zones.put(zone.name(), zone);
        records.putIfAbsent(zone.name(), new ArrayList<>());
        return this;
    }

    public List<String> calls() {
        return List.copyOf(calls);
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
    public List<DnsRecord> records(String zone) {
        calls.add("records:" + zone);
        return List.copyOf(requireZoneRecords(zone));
    }

    @Override
    public DnsRecord createRecord(String zone, DnsRecordSpec spec) {
        calls.add("createRecord:" + zone + ":" + spec.name());
        List<DnsRecord> zoneRecords = requireZoneRecords(zone);
        DnsRecord record = new DnsRecord(
                "rec-" + recordIdCounter.incrementAndGet(), zone, spec.name(), spec.type(), spec.value(), spec.ttl(),
                spec.priority());
        zoneRecords.add(record);
        return record;
    }

    @Override
    public DnsRecord updateRecord(String zone, String recordId, DnsRecordSpec spec) {
        calls.add("updateRecord:" + zone + ":" + recordId);
        List<DnsRecord> zoneRecords = requireZoneRecords(zone);
        DnsRecord existing = zoneRecords.stream().filter(r -> r.id().equals(recordId)).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No DNS record '" + recordId + "' in zone '" + zone + "'"));
        DnsRecord updated = new DnsRecord(recordId, zone, spec.name(), spec.type(), spec.value(), spec.ttl(), spec.priority());
        zoneRecords.set(zoneRecords.indexOf(existing), updated);
        return updated;
    }

    @Override
    public void deleteRecord(String zone, String recordId) {
        calls.add("deleteRecord:" + zone + ":" + recordId);
        List<DnsRecord> zoneRecords = requireZoneRecords(zone);
        boolean removed = zoneRecords.removeIf(r -> r.id().equals(recordId));
        if (!removed) {
            throw new ResourceNotFoundException("No DNS record '" + recordId + "' in zone '" + zone + "'");
        }
    }

    private List<DnsRecord> requireZoneRecords(String zone) {
        List<DnsRecord> zoneRecords = records.get(zone);
        if (zoneRecords == null) {
            throw new ResourceNotFoundException("No DNS zone '" + zone + "' on provider '" + name + "'");
        }
        return zoneRecords;
    }
}
