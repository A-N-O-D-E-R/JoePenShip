package io.virtualization.sdk.domain;

import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.dns.DnsProvider;
import io.virtualization.sdk.dns.DnsProviderRegistry;
import io.virtualization.sdk.dns.DnsRecord;
import io.virtualization.sdk.dns.DnsRecordSpec;
import io.virtualization.sdk.dns.DnsZone;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The only {@link DomainManager} implementation. Multi-provider by design — unlike {@code
 * virtualization-vps}'s single-provider-per-instance {@code VpsManager}, one {@code
 * DefaultDomainManager} composes a whole {@link DnsProviderRegistry}: {@code
 * virtualization.dns.providers.*} config is a named map, and a specific provider is associated
 * per domain via {@link Domain#dnsProvider()}, not fixed for the whole manager instance.
 */
public final class DefaultDomainManager implements DomainManager {

    private final DomainRepository repository;
    private final DnsProviderRegistry dnsProviders;

    public DefaultDomainManager(DomainRepository repository, DnsProviderRegistry dnsProviders) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.dnsProviders = Objects.requireNonNull(dnsProviders, "dnsProviders must not be null");
    }

    @Override
    public Domain register(String rawDomainName) {
        String normalized = DomainNames.normalize(rawDomainName);
        Domain domain = new Domain(DomainId.generate(), normalized, DomainStatus.ACTIVE, null, Instant.now());
        repository.save(domain);
        return domain;
    }

    @Override
    public Domain associateDnsProvider(DomainId id, String dnsProviderName) {
        Domain domain = get(id);
        dnsProviders.get(dnsProviderName); // validates existence; ConfigurationException propagates unrewrapped
        Domain updated = new Domain(domain.id(), domain.name(), domain.status(), dnsProviderName, domain.createdAt());
        repository.save(updated);
        return updated;
    }

    @Override
    public Domain get(DomainId id) {
        Objects.requireNonNull(id, "id must not be null");
        return repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("No domain with id '" + id.value() + "'"));
    }

    @Override
    public Optional<Domain> findByName(String name) {
        String normalized = DomainNames.normalize(name);
        return repository.findAll().stream().filter(d -> d.name().equals(normalized)).findFirst();
    }

    @Override
    public List<Domain> list() {
        return repository.findAll();
    }

    @Override
    public Optional<DnsZone> resolveZone(DomainId id) {
        Domain domain = get(id);
        DnsProvider provider = requireDnsProvider(domain);
        try {
            return Optional.of(walkToZone(provider, domain.name()).zone());
        } catch (ResourceNotFoundException e) {
            return Optional.empty();
        }
    }

    @Override
    public DnsRecord createRecord(DomainId id, DnsRecordSpec spec) {
        ResolvedZone resolved = requireZone(id);
        return resolved.provider().createRecord(resolved.zone().name(), spec);
    }

    @Override
    public DnsRecord updateRecord(DomainId id, String recordId, DnsRecordSpec spec) {
        ResolvedZone resolved = requireZone(id);
        return resolved.provider().updateRecord(resolved.zone().name(), recordId, spec);
    }

    @Override
    public void deleteRecord(DomainId id, String recordId) {
        ResolvedZone resolved = requireZone(id);
        resolved.provider().deleteRecord(resolved.zone().name(), recordId);
    }

    @Override
    public List<DnsRecord> listRecords(DomainId id) {
        ResolvedZone resolved = requireZone(id);
        return resolved.provider().records(resolved.zone().name());
    }

    private DnsProvider requireDnsProvider(Domain domain) {
        if (domain.dnsProvider() == null) {
            throw new IllegalStateException("Domain '" + domain.id().value() + "' has no associated DNS provider yet");
        }
        return dnsProviders.get(domain.dnsProvider());
    }

    private ResolvedZone requireZone(DomainId id) {
        Domain domain = get(id);
        DnsProvider provider = requireDnsProvider(domain);
        ZoneMatch match = walkToZone(provider, domain.name());
        return new ResolvedZone(match.zone(), provider);
    }

    /**
     * {@link DnsProvider#getZone} is an exact-match lookup — a registered domain is usually a
     * subdomain of the zone that actually owns it (e.g. {@code app.example.com} under a zone
     * registered as {@code example.com}), so walk up the label chain until one matches. Mirrors
     * {@code Dns01AcmeProvider.resolveZone} (virtualization-acme-mock), duplicated rather than
     * shared — same small-helper-duplication precedent used throughout this codebase.
     */
    private static ZoneMatch walkToZone(DnsProvider provider, String domain) {
        String candidate = domain;
        StringBuilder label = new StringBuilder();
        while (true) {
            Optional<DnsZone> zone = provider.getZone(candidate);
            if (zone.isPresent()) {
                return new ZoneMatch(zone.get(), label.toString());
            }
            int dot = candidate.indexOf('.');
            if (dot < 0) {
                throw new ResourceNotFoundException("No DNS zone for domain '" + domain + "'");
            }
            if (!label.isEmpty()) {
                label.append('.');
            }
            label.append(candidate, 0, dot);
            candidate = candidate.substring(dot + 1);
        }
    }

    private record ResolvedZone(DnsZone zone, DnsProvider provider) {}

    private record ZoneMatch(DnsZone zone, String label) {}
}
