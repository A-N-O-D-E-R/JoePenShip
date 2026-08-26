package io.virtualization.sdk.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Binds:
 * <pre>{@code
 * virtualization:
 *   certificates:
 *     enabled: true
 *     providers:
 *       letsencrypt:
 *         type: mock
 *         dns-provider: cloudflare   # names an entry under virtualization.dns.providers
 * }</pre>
 * Only {@code type: mock} exists today (backed by {@code virtualization-acme-mock}'s {@code
 * Dns01AcmeProvider}, composing the named DNS provider) — no real Let's Encrypt/ZeroSSL provider
 * is implemented yet.
 */
@ConfigurationProperties(prefix = "virtualization.certificates")
public record CertificateProperties(boolean enabled, Map<String, ProviderEntry> providers) {

    public CertificateProperties {
        providers = providers != null ? Map.copyOf(providers) : Map.of();
    }

    public record ProviderEntry(String type, String dnsProvider) {}
}
