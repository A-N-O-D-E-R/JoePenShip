package io.sablier.core;

import io.sablier.core.exception.WorkloadNotFoundException;

import java.util.List;

/** A backend capable of listing and controlling workloads (Incus, and later Docker/Podman/Proxmox/...). */
public interface WorkloadProvider {

    String name();

    /**
     * @throws WorkloadNotFoundException if no workload with the given id exists
     */
    Workload get(String id);

    List<Workload> list();

    /** @return the workloads discovered under the given group; empty if none match */
    List<Workload> findByGroup(String group);

    Operation start(String id);

    Operation stop(String id);

    WorkloadState state(String id);

    ReadinessStatus readiness(String id);
}
