package io.virtualization.sdk.dns;

import io.virtualization.sdk.core.exception.ResourceNotFoundException;

import java.util.List;
import java.util.Optional;

/**
 * A DNS backend (Cloudflare, Route53, ...). Implementations live in their own provider modules —
 * this module never depends on one. {@code zone} parameters below take a zone's {@link
 * DnsZone#name()}, not the provider's own internal zone id.
 */
public interface DnsProvider {

    String name();

    List<DnsZone> zones();

    Optional<DnsZone> getZone(String domain);

    /** @throws ResourceNotFoundException if {@code zone} is unknown */
    List<DnsRecord> records(String zone);

    /** @throws ResourceNotFoundException if {@code zone} is unknown */
    DnsRecord createRecord(String zone, DnsRecordSpec spec);

    /** @throws ResourceNotFoundException if {@code zone} or {@code recordId} is unknown */
    DnsRecord updateRecord(String zone, String recordId, DnsRecordSpec spec);

    /** @throws ResourceNotFoundException if {@code zone} or {@code recordId} is unknown */
    void deleteRecord(String zone, String recordId);
}
