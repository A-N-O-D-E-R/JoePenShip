package io.sablier.core;

import java.util.List;
import java.util.Objects;

/** A named group and the workloads discovered under it (see {@link WorkloadProvider#findByGroup(String)}). */
public record WorkloadGroup(String name, List<Workload> workloads) {

    public WorkloadGroup {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(workloads, "workloads must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        workloads = List.copyOf(workloads);
    }
}
