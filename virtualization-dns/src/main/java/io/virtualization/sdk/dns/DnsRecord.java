package io.virtualization.sdk.dns;

import java.util.Objects;

/**
 * A DNS record as a provider returns it. {@code id}/{@code zone} are provider-native opaque
 * strings (matching {@link DnsProvider}'s own method shapes), not SDK-generated identities like
 * {@code DomainId}.
 *
 * @param zone the owning {@link DnsZone#name()}
 */
public record DnsRecord(String id, String zone, String name, DnsRecordType type, String value, Long ttl, Integer priority) {

    public DnsRecord {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(zone, "zone must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(value, "value must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (zone.isBlank()) {
            throw new IllegalArgumentException("zone must not be blank");
        }
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
        if (ttl != null && ttl < 0) {
            throw new IllegalArgumentException("ttl must not be negative, was " + ttl);
        }
        if (priority != null && priority < 0) {
            throw new IllegalArgumentException("priority must not be negative, was " + priority);
        }
    }
}
