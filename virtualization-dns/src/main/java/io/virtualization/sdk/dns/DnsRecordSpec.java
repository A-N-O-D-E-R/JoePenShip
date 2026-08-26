package io.virtualization.sdk.dns;

import java.util.Objects;

/**
 * What a caller supplies to create or update a {@link DnsRecord} — everything except {@code id}
 * and {@code zone}, which {@link DnsProvider#createRecord}/{@link DnsProvider#updateRecord} take
 * as separate parameters.
 *
 * @param ttl      seconds, {@code null} for the provider's own default
 * @param priority only meaningful for {@link DnsRecordType#MX} (and, later, {@code SRV}); {@code
 *                 null} otherwise
 */
public record DnsRecordSpec(String name, DnsRecordType type, String value, Long ttl, Integer priority) {

    public DnsRecordSpec {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(value, "value must not be null");
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
