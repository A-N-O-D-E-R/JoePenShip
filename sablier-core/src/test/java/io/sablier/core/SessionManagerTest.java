package io.sablier.core;

import io.sablier.core.exception.SessionNotFoundException;
import io.sablier.core.exception.WorkloadNotFoundException;
import io.sablier.core.support.FakeWorkloadProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionManagerTest {

    private static final SessionPolicy POLICY = new SessionPolicy(Duration.ofMinutes(30), Duration.ofHours(8));
    private static final ReadinessPolicy READINESS_POLICY = new ReadinessPolicy(Duration.ofSeconds(2), Duration.ofMillis(10), 20);

    private FakeWorkloadProvider provider;
    private InMemorySessionRepository repository;
    private SessionManager manager;

    @BeforeEach
    void setUp() {
        provider = new FakeWorkloadProvider();
        repository = new InMemorySessionRepository();
        manager = new SessionManager(provider, repository, POLICY, READINESS_POLICY);
    }

    private static Workload workload(String id, String group, WorkloadState state) {
        return new Workload(id, id, WorkloadType.CONTAINER, state, group, "default", java.util.Optional.empty());
    }

    @Test
    void createSessionStartsAStoppedWorkloadExactlyOnce() {
        provider.withWorkload(workload("jellyfin", "media", WorkloadState.STOPPED));

        Session session = manager.createSession(new SessionRequest("media", Duration.ofMinutes(30)));

        assertThat(provider.startCallCount("jellyfin")).isEqualTo(1);
        assertThat(session.status()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(session.workloadId()).hasValue("jellyfin");
    }

    @Test
    void createSessionThrowsWhenWorkloadNeverBecomesReady() {
        provider.withWorkload(workload("jellyfin", "media", WorkloadState.STOPPED));
        provider.neverReady("jellyfin");

        assertThatThrownBy(() -> manager.createSession(new SessionRequest("media", Duration.ofMinutes(30))))
                .isInstanceOf(io.sablier.core.exception.OperationException.class);
        assertThat(provider.startCallCount("jellyfin")).isEqualTo(1);
    }

    @Test
    void createSessionOnAlreadyRunningWorkloadNeverStartsIt() {
        provider.withWorkload(workload("jellyfin", "media", WorkloadState.RUNNING));

        manager.createSession(new SessionRequest("media", Duration.ofMinutes(30)));

        assertThat(provider.startCallCount("jellyfin")).isZero();
    }

    @Test
    void createSessionThrowsWorkloadNotFoundForEmptyGroup() {
        assertThatThrownBy(() -> manager.createSession(new SessionRequest("nonexistent", Duration.ofMinutes(30))))
                .isInstanceOf(WorkloadNotFoundException.class);
    }

    @Test
    void createSessionPicksFirstWorkloadWhenGroupResolvesToMultiple() {
        provider.withWorkload(workload("jellyfin", "media", WorkloadState.STOPPED));
        provider.withWorkload(workload("sonarr", "media", WorkloadState.STOPPED));

        Session session = manager.createSession(new SessionRequest("media", Duration.ofMinutes(30)));

        assertThat(session.workloadId()).isIn(java.util.Optional.of("jellyfin"), java.util.Optional.of("sonarr"));
        assertThat(provider.findByGroup("media")).hasSize(2);
    }

    @Test
    void getSessionThrowsForUnknownId() {
        assertThatThrownBy(() -> manager.getSession("missing")).isInstanceOf(SessionNotFoundException.class);
    }

    @Test
    void extendSessionPushesExpiryForward() {
        provider.withWorkload(workload("jellyfin", "media", WorkloadState.STOPPED));
        Session session = manager.createSession(new SessionRequest("media", Duration.ofMinutes(30)));

        Session extended = manager.extendSession(session.id(), Duration.ofMinutes(10));

        assertThat(extended.expiresAt()).isAfter(session.expiresAt());
    }

    @Test
    void extendSessionClampsToMaxDurationFromCreation() {
        provider.withWorkload(workload("jellyfin", "media", WorkloadState.STOPPED));
        Session session = manager.createSession(new SessionRequest("media", Duration.ofMinutes(30)));

        Session extended = manager.extendSession(session.id(), Duration.ofDays(1));

        assertThat(extended.expiresAt()).isEqualTo(session.createdAt().plus(POLICY.maxDuration()));
    }

    @Test
    void extendSessionNeverCallsStartAgain() {
        provider.withWorkload(workload("jellyfin", "media", WorkloadState.STOPPED));
        Session session = manager.createSession(new SessionRequest("media", Duration.ofMinutes(30)));

        manager.extendSession(session.id(), Duration.ofMinutes(10));

        assertThat(provider.startCallCount("jellyfin")).isEqualTo(1);
    }

    @Test
    void workloadStaysUpWhileEitherOfTwoSessionsIsActive() {
        provider.withWorkload(workload("jellyfin", "media", WorkloadState.STOPPED));
        Session sessionA = manager.createSession(new SessionRequest("media", Duration.ofMinutes(30)));
        Session sessionB = manager.createSession(new SessionRequest("media", Duration.ofMinutes(20)));

        assertThat(provider.startCallCount("jellyfin")).isEqualTo(1);

        manager.expireSession(sessionA.id());
        assertThat(provider.stopCallCount("jellyfin")).isZero();

        manager.expireSession(sessionB.id());
        assertThat(provider.stopCallCount("jellyfin")).isEqualTo(1);
    }

    @Test
    void workloadAlreadyRunningBeforeSablierIsNeverStopped() {
        provider.withWorkload(workload("jellyfin", "media", WorkloadState.RUNNING));
        Session session = manager.createSession(new SessionRequest("media", Duration.ofMinutes(30)));

        manager.terminateSession(session.id());

        assertThat(provider.stopCallCount("jellyfin")).isZero();
    }

    @Test
    void terminateSessionTwiceIsIdempotent() {
        provider.withWorkload(workload("jellyfin", "media", WorkloadState.STOPPED));
        Session session = manager.createSession(new SessionRequest("media", Duration.ofMinutes(30)));

        manager.terminateSession(session.id());
        manager.terminateSession(session.id());

        assertThat(provider.stopCallCount("jellyfin")).isEqualTo(1);
        assertThat(manager.getSession(session.id()).status()).isEqualTo(SessionStatus.TERMINATED);
    }

    @Test
    void expireSessionTwiceIsIdempotent() {
        provider.withWorkload(workload("jellyfin", "media", WorkloadState.STOPPED));
        Session session = manager.createSession(new SessionRequest("media", Duration.ofMinutes(30)));

        manager.expireSession(session.id());
        manager.expireSession(session.id());

        assertThat(provider.stopCallCount("jellyfin")).isEqualTo(1);
    }

    @Test
    void concurrentCreateSessionForSameGroupStartsWorkloadExactlyOnce() throws InterruptedException {
        provider.withWorkload(workload("jellyfin", "media", WorkloadState.STOPPED));
        int threadCount = 16;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        List<Session> results = new java.util.concurrent.CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    go.await();
                    Session session = manager.createSession(new SessionRequest("media", Duration.ofMinutes(30)));
                    results.add(session);
                    successCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        ready.await();
        go.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        assertThat(provider.startCallCount("jellyfin")).isEqualTo(1);
        assertThat(successCount.get()).isEqualTo(threadCount);
        assertThat(results.stream().map(Session::id).distinct().count()).isEqualTo(threadCount);
        assertThat(repository.findActive()).hasSize(threadCount);
    }
}
