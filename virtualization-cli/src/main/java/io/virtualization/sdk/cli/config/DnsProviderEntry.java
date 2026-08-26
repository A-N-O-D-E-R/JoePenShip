package io.virtualization.sdk.cli.config;

import java.util.List;

/** {@code virtualization.dns.providers.<name>} — single-shape, unlike the multi-type {@link ProviderEntry}. */
public record DnsProviderEntry(String type, List<String> zones) {}
