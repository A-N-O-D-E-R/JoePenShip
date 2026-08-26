package io.virtualization.sdk.domain;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

/**
 * A provider-neutral, registered domain. {@code name} must already be normalized (see {@link
 * DomainNames#normalize}) — this record validates that invariant, it does not transform its
 * input. {@code dnsProvider} is {@code null} until {@link DomainManager#associateDnsProvider}
 * names one of the configured {@code virtualization-dns} {@code DnsProviderRegistry} entries.
 */
public record Domain(DomainId id, String name, DomainStatus status, String dnsProvider, Instant createdAt) {

    public Domain {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (!name.equals(name.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("name must already be lower-case, was '" + name + "'");
        }
        if (name.endsWith(".")) {
            throw new IllegalArgumentException("name must not have a trailing dot, was '" + name + "'");
        }
    }
}
