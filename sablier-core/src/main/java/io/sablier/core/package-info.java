/**
 * Provider-neutral workload activation core: domain model, {@link io.sablier.core.WorkloadProvider}
 * abstraction, session lifecycle orchestration, and asynchronous operation handling. Pure Java,
 * no framework dependencies — provider modules (Incus, and later Docker/Podman/Proxmox) and
 * consumer modules (REST API, CLI, Spring Boot starter) depend on this module, never the reverse.
 */
package io.sablier.core;
