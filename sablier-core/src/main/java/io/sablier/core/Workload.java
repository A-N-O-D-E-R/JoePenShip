package io.sablier.core;

import java.util.Objects;
import java.util.Optional;

/**
 * A provider-neutral view of a single container or virtual machine.
 *
 * @param project  the provider-scoped namespace this workload belongs to (e.g. an Incus
 *                 project). Every provider has some notion of this, even if it's a constant
 *                 like {@code "default"} for a provider that doesn't distinguish namespaces —
 *                 kept separate from {@link #group()}, which is Sablier's own cross-cutting
 *                 activation grouping, not a provider concept.
 * @param location the cluster member (or equivalent) this workload runs on, when the provider
 *                 has that concept and it's known; empty otherwise. Never encoded into {@link
 *                 #id()} — see {@code IncusWorkloadProvider}'s Javadoc for why identity fields
 *                 stay structured rather than flattened into the id string.
 */
public record Workload(
        String id, String name, WorkloadType type, WorkloadState state, String group, String project, Optional<String> location) {

    public Workload {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(group, "group must not be null");
        Objects.requireNonNull(project, "project must not be null");
        Objects.requireNonNull(location, "location must not be null (use Optional.empty())");
        if (id.isBlank()) {
            throw new IllegalArgumentException("id must not be blank");
        }
        if (group.isBlank()) {
            throw new IllegalArgumentException("group must not be blank");
        }
        if (project.isBlank()) {
            throw new IllegalArgumentException("project must not be blank");
        }
    }
}
