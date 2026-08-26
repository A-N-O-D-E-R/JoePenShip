package io.sablier.core;

import io.sablier.core.exception.OperationException;
import io.sablier.core.exception.SessionNotFoundException;
import io.sablier.core.exception.WorkloadNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Orchestrates the create/extend/expire/terminate session lifecycle against a {@link
 * WorkloadProvider}: starts a workload the first time a session needs it, tracks how many active
 * sessions currently claim it, and stops it only once that count reaches zero — and only if
 * Sablier itself started it (never a workload that was already running before Sablier touched
 * it).
 *
 * <p><b>Concurrency</b>: a per-workload lock (the workload's own {@link WorkloadBookkeeping}
 * instance, created race-free via {@code computeIfAbsent}) serializes start/stop decisions for
 * that one workload without contending with operations on any other workload. This is a
 * single-JVM lock providing basic correctness, not the fuller request-coalescing / distributed
 * dedup a later phase's scheduler work will add.
 *
 * <p><b>Blocking</b>: {@link #createSession(SessionRequest)} blocks on {@link
 * Operation#await()} for the workload's start operation, and — only when this call is the one
 * that actually started the workload (not when attaching to an already-running one) — on {@link
 * ReadinessAwaiter#await} for it to report ready, before returning. Keeping the HTTP API
 * response non-blocking is a REST-layer (later phase) concern, not this class's.
 */
public final class SessionManager {

    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);

    private final WorkloadProvider provider;
    private final SessionRepository repository;
    private final SessionPolicy policy;
    private final ReadinessPolicy readinessPolicy;
    private final Map<String, WorkloadBookkeeping> bookkeeping = new ConcurrentHashMap<>();

    public SessionManager(WorkloadProvider provider, SessionRepository repository, SessionPolicy policy, ReadinessPolicy readinessPolicy) {
        this.provider = Objects.requireNonNull(provider, "provider must not be null");
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
        this.policy = Objects.requireNonNull(policy, "policy must not be null");
        this.readinessPolicy = Objects.requireNonNull(readinessPolicy, "readinessPolicy must not be null");
    }

    public Session createSession(SessionRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        List<Workload> matches = provider.findByGroup(request.group());
        if (matches.isEmpty()) {
            throw new WorkloadNotFoundException("No workload found for group '" + request.group() + "'");
        }
        // V1 simplification: activate the first discovered workload only. Full multi-workload
        // group activation (starting every workload in the group together) is a later phase.
        Workload workload = matches.get(0);

        WorkloadBookkeeping bk = bookkeeping.computeIfAbsent(workload.id(), id -> new WorkloadBookkeeping());
        synchronized (bk) {
            if (bk.activeSessionCount == 0) {
                WorkloadState state = provider.state(workload.id());
                if (state != WorkloadState.RUNNING && state != WorkloadState.READY) {
                    Operation operation = provider.start(workload.id());
                    if (operation.await() == OperationStatus.FAILED) {
                        throw new OperationException("Failed to start workload '" + workload.id() + "'"
                                + operation.error().map(e -> ": " + e.getMessage()).orElse(""));
                    }
                    bk.startedBySablier = true;
                    log.info("workload={} group={} started by sablier, awaiting readiness", workload.id(), request.group());

                    // Only wait for readiness on the transition we ourselves triggered (section 2's
                    // flow: "already running" skips straight to READY, only "stopped -> START" waits).
                    ReadinessStatus readiness = ReadinessAwaiter.await(() -> provider.readiness(workload.id()), readinessPolicy);
                    if (readiness.state() != ReadinessState.READY) {
                        throw new OperationException("Workload '" + workload.id() + "' did not become ready: " + readiness.message());
                    }
                    log.info("workload={} group={} ready", workload.id(), request.group());
                } else {
                    bk.startedBySablier = false;
                    log.info("workload={} group={} already running, attaching session", workload.id(), request.group());
                }
            }
            bk.activeSessionCount++;
        }

        Instant now = Instant.now();
        Instant expiresAt = clampToMax(now, now.plus(request.duration()));
        Session session = new Session(
                UUID.randomUUID().toString(), request.group(), Optional.of(workload.id()), now, expiresAt, SessionStatus.ACTIVE);
        repository.save(session);
        log.info("session={} group={} workload={} expiresAt={} created", session.id(), session.group(), workload.id(), expiresAt);
        return session;
    }

    public Session getSession(String id) {
        return repository.findById(id).orElseThrow(() -> new SessionNotFoundException("No session with id '" + id + "'"));
    }

    public Session extendSession(String id, Duration extension) {
        Objects.requireNonNull(extension, "extension must not be null");
        Session session = getSession(id);
        Instant expiresAt = clampToMax(session.createdAt(), session.expiresAt().plus(extension));
        Session extended = new Session(
                session.id(), session.group(), session.workloadId(), session.createdAt(), expiresAt, session.status());
        repository.save(extended);
        log.info("session={} extended to expiresAt={}", id, expiresAt);
        return extended;
    }

    public void expireSession(String id) {
        endSession(id, SessionStatus.EXPIRED);
    }

    public void terminateSession(String id) {
        endSession(id, SessionStatus.TERMINATED);
    }

    private void endSession(String id, SessionStatus terminalStatus) {
        Session session = getSession(id);
        if (session.status().isTerminal()) {
            return; // idempotent: already ended
        }
        repository.save(new Session(
                session.id(), session.group(), session.workloadId(), session.createdAt(), session.expiresAt(), terminalStatus));
        log.info("session={} status={}", id, terminalStatus);

        session.workloadId().ifPresent(workloadId -> releaseWorkload(workloadId));
    }

    private void releaseWorkload(String workloadId) {
        WorkloadBookkeeping bk = bookkeeping.get(workloadId);
        if (bk == null) {
            return; // defensive: should not happen, every activation creates its bookkeeping first
        }
        synchronized (bk) {
            bk.activeSessionCount = Math.max(0, bk.activeSessionCount - 1);
            if (bk.activeSessionCount == 0 && bk.startedBySablier) {
                Operation operation = provider.stop(workloadId);
                if (operation.await() == OperationStatus.FAILED) {
                    log.warn("Failed to stop workload '{}' after last session ended: {}",
                            workloadId, operation.error().map(Throwable::getMessage).orElse("unknown error"));
                }
                bk.startedBySablier = false;
                log.info("workload={} stopped, no active sessions remain", workloadId);
            }
        }
    }

    private Instant clampToMax(Instant createdAt, Instant candidate) {
        Instant maxExpiry = createdAt.plus(policy.maxDuration());
        return candidate.isAfter(maxExpiry) ? maxExpiry : candidate;
    }

    private static final class WorkloadBookkeeping {
        int activeSessionCount;
        boolean startedBySablier;
    }
}
