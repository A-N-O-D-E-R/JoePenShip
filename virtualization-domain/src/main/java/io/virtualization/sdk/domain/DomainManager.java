package io.virtualization.sdk.domain;

import io.virtualization.sdk.core.exception.ConfigurationException;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.dns.DnsRecord;
import io.virtualization.sdk.dns.DnsRecordSpec;
import io.virtualization.sdk.dns.DnsZone;

import java.util.List;
import java.util.Optional;

public interface DomainManager {

    /** Normalizes {@code rawDomainName} (see {@link DomainNames#normalize}) and registers it, with no DNS provider associated yet. */
    Domain register(String rawDomainName);

    /**
     * @throws ResourceNotFoundException if {@code id} is unknown
     * @throws ConfigurationException    if {@code dnsProviderName} isn't a registered DNS provider
     */
    Domain associateDnsProvider(DomainId id, String dnsProviderName);

    /** @throws ResourceNotFoundException if {@code id} is unknown */
    Domain get(DomainId id);

    /** Normalizes {@code name} before comparing. Non-throwing — an unknown name is a normal, expected case (e.g. a REST 404), not exceptional. */
    Optional<Domain> findByName(String name);

    List<Domain> list();

    /**
     * @throws ResourceNotFoundException if {@code id} is unknown
     * @throws IllegalStateException     if {@code id} has no associated DNS provider yet
     */
    Optional<DnsZone> resolveZone(DomainId id);

    /**
     * @throws ResourceNotFoundException if {@code id} or its zone is unknown
     * @throws IllegalStateException     if {@code id} has no associated DNS provider yet
     */
    DnsRecord createRecord(DomainId id, DnsRecordSpec spec);

    /**
     * @throws ResourceNotFoundException if {@code id} or its zone is unknown
     * @throws IllegalStateException     if {@code id} has no associated DNS provider yet
     */
    DnsRecord updateRecord(DomainId id, String recordId, DnsRecordSpec spec);

    /**
     * @throws ResourceNotFoundException if {@code id} or its zone is unknown
     * @throws IllegalStateException     if {@code id} has no associated DNS provider yet
     */
    void deleteRecord(DomainId id, String recordId);

    /**
     * @throws ResourceNotFoundException if {@code id} or its zone is unknown
     * @throws IllegalStateException     if {@code id} has no associated DNS provider yet
     */
    List<DnsRecord> listRecords(DomainId id);
}
