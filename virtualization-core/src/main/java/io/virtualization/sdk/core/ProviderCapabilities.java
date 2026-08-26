package io.virtualization.sdk.core;

import java.util.EnumSet;
import java.util.Set;

/** The set of {@link Capability} values a {@link VirtualizationProvider} supports. */
public final class ProviderCapabilities {

    private final Set<Capability> capabilities;

    private ProviderCapabilities(Set<Capability> capabilities) {
        this.capabilities = capabilities;
    }

    public static ProviderCapabilities of(Capability... capabilities) {
        EnumSet<Capability> set = EnumSet.noneOf(Capability.class);
        for (Capability capability : capabilities) {
            set.add(capability);
        }
        return new ProviderCapabilities(Set.copyOf(set));
    }

    public boolean supports(Capability capability) {
        return capabilities.contains(capability);
    }

    public Set<Capability> all() {
        return capabilities;
    }
}
