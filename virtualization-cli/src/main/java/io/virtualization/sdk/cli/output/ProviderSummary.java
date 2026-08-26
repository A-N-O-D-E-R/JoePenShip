package io.virtualization.sdk.cli.output;

import io.virtualization.sdk.core.Capability;

import java.util.Set;

/** CLI-facing view of a configured provider, for {@code provider list}. */
public record ProviderSummary(String name, String type, Set<Capability> capabilities) {}
