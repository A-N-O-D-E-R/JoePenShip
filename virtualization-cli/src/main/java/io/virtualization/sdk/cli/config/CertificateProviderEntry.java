package io.virtualization.sdk.cli.config;

/** {@code virtualization.certificates.providers.<name>} — {@code dnsProvider} maps to YAML's kebab-case {@code dns-provider}. */
public record CertificateProviderEntry(String type, String dnsProvider) {}
