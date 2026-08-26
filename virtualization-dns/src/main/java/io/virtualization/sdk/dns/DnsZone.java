package io.virtualization.sdk.dns;

import java.util.Objects;

/**
 * A DNS zone as a provider sees it — e.g. {@code example.com}. Subdomains ({@code
 * app.example.com}) are {@link DnsRecord}s inside a zone, not separate zones.
 *
 * @param name       the zone name (e.g. {@code example.com})
 * @param provider   the {@link DnsProvider#name()} this zone belongs to
 * @param providerId the provider's own opaque zone identifier
 */
public record DnsZone(String name, String provider, String providerId) {

    public DnsZone {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(provider, "provider must not be null");
        Objects.requireNonNull(providerId, "providerId must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (provider.isBlank()) {
            throw new IllegalArgumentException("provider must not be blank");
        }
        if (providerId.isBlank()) {
            throw new IllegalArgumentException("providerId must not be blank");
        }
    }
}
