package io.virtualization.sdk.vps;

import io.virtualization.sdk.core.ComputeResources;
import io.virtualization.sdk.core.Operation;
import io.virtualization.sdk.core.OperationStatus;
import io.virtualization.sdk.core.exception.OperationException;
import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.core.image.ImageReference;
import io.virtualization.sdk.vps.support.FakeVpsProvisioner;
import io.virtualization.sdk.vps.support.FakeVpsReadinessChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultVpsManagerTest {

    private static final ImageReference IMAGE = new ImageReference("incus", "images", "ubuntu/24.04");
    private static final ImageReference OTHER_IMAGE = new ImageReference("incus", "images", "debian/13");

    private InMemoryVpsRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryVpsRepository();
    }

    private VpsManager manager(FakeVpsProvisioner provisioner) {
        return new DefaultVpsManager(repository, provisioner);
    }

    private VpsManager manager(FakeVpsProvisioner provisioner, VpsReadinessChecker readinessChecker) {
        return new DefaultVpsManager(repository, provisioner, readinessChecker);
    }

    private VpsId createReady(VpsManager manager, String name) {
        CreateVpsOperation operation = manager.create(VpsSpec.builder(name, IMAGE).build());
        operation.await();
        return operation.vpsId();
    }

    @Nested
    class Lifecycle {

        @Test
        void createReconcilesToReadyOnSuccess() {
            VpsManager manager = manager(FakeVpsProvisioner.succeeding());

            CreateVpsOperation operation = manager.create(VpsSpec.builder("web-01", IMAGE).build());
            operation.await();

            assertThat(operation.status()).isEqualTo(OperationStatus.SUCCEEDED);
            Vps vps = manager.get(operation.vpsId());
            assertThat(vps.state()).isEqualTo(VpsState.READY);
            assertThat(vps.provider()).isEqualTo("fake");
            assertThat(vps.workloadId()).isNotBlank();
        }

        @Test
        void createReconcilesToErrorOnFailure() {
            VpsManager manager = manager(FakeVpsProvisioner.failing(new OperationException("boom")));

            CreateVpsOperation operation = manager.create(VpsSpec.builder("web-01", IMAGE).build());
            operation.await();

            assertThat(manager.get(operation.vpsId()).state()).isEqualTo(VpsState.ERROR);
        }

        @Test
        void startStopRestartShutdownDestroyEachReconcileToTheRightTerminalState() {
            VpsManager manager = manager(FakeVpsProvisioner.succeeding());
            VpsId id = createReady(manager, "web-01");

            // READY -> STOPPING -> STOPPED
            Operation stop = manager.stop(id);
            stop.await();
            assertThat(manager.get(id).state()).isEqualTo(VpsState.STOPPED);

            // STOPPED -> STARTING -> RUNNING
            Operation start = manager.start(id);
            start.await();
            assertThat(manager.get(id).state()).isEqualTo(VpsState.RUNNING);

            // RUNNING -> STARTING -> RUNNING
            Operation restart = manager.restart(id);
            restart.await();
            assertThat(manager.get(id).state()).isEqualTo(VpsState.RUNNING);

            // RUNNING -> STOPPING -> STOPPED (shutdown)
            Operation shutdown = manager.shutdown(id);
            shutdown.await();
            assertThat(manager.get(id).state()).isEqualTo(VpsState.STOPPED);

            // STOPPED -> DESTROYING -> DESTROYED
            Operation destroy = manager.destroy(id);
            destroy.await();
            assertThat(manager.get(id).state()).isEqualTo(VpsState.DESTROYED);
            assertThat(manager.get(id).destroyedAt()).isNotNull();
        }
    }

    @Nested
    class StateTransitionsAndValidation {

        @Test
        void startFromReadyIsRejected() {
            // READY means "freshly provisioned and already running" — start()'s only legal source
            // is STOPPED (see the class-level table); restart() is the READY/RUNNING equivalent.
            VpsManager manager = manager(FakeVpsProvisioner.succeeding());
            VpsId id = createReady(manager, "web-01");

            assertThatThrownBy(() -> manager.start(id)).isInstanceOf(InvalidVpsStateException.class);
        }

        @Test
        void stopFromStoppedIsRejected() {
            VpsManager manager = manager(FakeVpsProvisioner.succeeding());
            VpsId id = createReady(manager, "web-01");
            manager.stop(id).await();

            assertThatThrownBy(() -> manager.stop(id)).isInstanceOf(InvalidVpsStateException.class);
        }

        @Test
        void destroyFromDestroyedIsRejected() {
            VpsManager manager = manager(FakeVpsProvisioner.succeeding());
            VpsId id = createReady(manager, "web-01");
            manager.destroy(id).await();

            assertThatThrownBy(() -> manager.destroy(id)).isInstanceOf(InvalidVpsStateException.class);
        }

        @Test
        void getUnknownIdThrowsResourceNotFound() {
            VpsManager manager = manager(FakeVpsProvisioner.succeeding());

            assertThatThrownBy(() -> manager.get(VpsId.generate())).isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class Rebuild {

        @Test
        void rebuildUpdatesImageAndReturnsToReady() {
            VpsManager manager = manager(FakeVpsProvisioner.succeeding());
            VpsId id = createReady(manager, "web-01");

            CreateVpsOperation operation = manager.rebuild(id, OTHER_IMAGE);
            operation.await();

            Vps vps = manager.get(id);
            assertThat(vps.state()).isEqualTo(VpsState.READY);
            assertThat(vps.image()).isEqualTo(OTHER_IMAGE);
        }

        @Test
        void rebuildFromRunningIsRejected() {
            VpsManager manager = manager(FakeVpsProvisioner.succeeding());
            VpsId id = createReady(manager, "web-01");
            manager.stop(id).await();
            manager.start(id).await();

            assertThatThrownBy(() -> manager.rebuild(id, OTHER_IMAGE)).isInstanceOf(InvalidVpsStateException.class);
        }
    }

    @Nested
    class ResourceCalculation {

        @Test
        void omittedSpecFieldsGetPhase1Defaults() {
            VpsManager manager = manager(FakeVpsProvisioner.succeeding());

            CreateVpsOperation operation = manager.create(VpsSpec.builder("web-01", IMAGE).build());
            operation.await();
            Vps vps = manager.get(operation.vpsId());

            assertThat(vps.compute()).isEqualTo(new ComputeResources(1, 1_024));
            assertThat(vps.storage()).isEqualTo(new StorageConfiguration(DataSize.ofGigabytes(10)));
            assertThat(vps.network()).isEqualTo(NetworkConfiguration.UNSPECIFIED);
        }

        @Test
        void explicitSpecFieldsPassThroughUnchanged() {
            VpsManager manager = manager(FakeVpsProvisioner.succeeding());
            ComputeResources compute = new ComputeResources(4, 8_192);
            StorageConfiguration storage = new StorageConfiguration(DataSize.ofGigabytes(80));

            CreateVpsOperation operation = manager.create(
                    VpsSpec.builder("web-01", IMAGE).compute(compute).storage(storage).build());
            operation.await();
            Vps vps = manager.get(operation.vpsId());

            assertThat(vps.compute()).isEqualTo(compute);
            assertThat(vps.storage()).isEqualTo(storage);
        }
    }

    @Nested
    class Readiness {

        @Test
        void readyOnFirstAttemptFlipsToReadyImmediately() {
            FakeVpsReadinessChecker checker = FakeVpsReadinessChecker.readyOnAttempt(1);
            VpsManager manager = manager(FakeVpsProvisioner.succeeding(), checker);

            CreateVpsOperation operation = manager.create(VpsSpec.builder("web-01", IMAGE).build());
            operation.await();

            assertThat(manager.get(operation.vpsId()).state()).isEqualTo(VpsState.READY);
            assertThat(checker.callCount()).isEqualTo(1);
        }

        @Test
        void readyAfterRetriesEventuallyFlipsToReady() {
            FakeVpsReadinessChecker checker = FakeVpsReadinessChecker.readyOnAttempt(3);
            VpsManager manager = manager(FakeVpsProvisioner.succeeding(), checker);

            CreateVpsOperation operation = manager.create(VpsSpec.builder("web-01", IMAGE).build());
            operation.await();

            assertThat(manager.get(operation.vpsId()).state()).isEqualTo(VpsState.READY);
            assertThat(checker.callCount()).isEqualTo(3);
        }

        @Test
        void neverReadyExhaustsAttemptsAndLandsInError() {
            FakeVpsReadinessChecker checker = FakeVpsReadinessChecker.neverReady();
            VpsManager manager = manager(FakeVpsProvisioner.succeeding(), checker);

            CreateVpsOperation operation = manager.create(VpsSpec.builder("web-01", IMAGE).build());
            operation.await();

            Vps vps = manager.get(operation.vpsId());
            assertThat(vps.state()).isEqualTo(VpsState.ERROR);
            assertThat(checker.callCount()).isEqualTo(3); // READINESS_MAX_ATTEMPTS
            // provisioning did succeed — the workload id is retained even though readiness failed.
            assertThat(vps.workloadId()).isNotBlank();
        }

        @Test
        void rebuildAlsoGoesThroughReadinessCheck() {
            FakeVpsReadinessChecker checker = FakeVpsReadinessChecker.readyOnAttempt(1);
            VpsManager manager = manager(FakeVpsProvisioner.succeeding(), checker);
            VpsId id = createReady(manager, "web-01");
            assertThat(manager.get(id).state()).isEqualTo(VpsState.READY); // reconciles create first

            checker.neverReadyFromNowOn();
            CreateVpsOperation operation = manager.rebuild(id, OTHER_IMAGE);
            operation.await();

            assertThat(manager.get(id).state()).isEqualTo(VpsState.ERROR);
        }
    }

    @Nested
    class Idempotency {

        @Test
        void sameIdempotencyKeyReturnsTheSameOperationAndCreatesOneRow() {
            FakeVpsProvisioner provisioner = FakeVpsProvisioner.succeeding();
            VpsManager manager = manager(provisioner);
            VpsSpec spec = VpsSpec.builder("web-01", IMAGE).idempotencyKey("key-1").build();

            CreateVpsOperation first = manager.create(spec);
            CreateVpsOperation second = manager.create(spec);

            assertThat(second).isSameAs(first);
            assertThat(provisioner.createCallCount()).isEqualTo(1);
            assertThat(repository.findAll()).hasSize(1);
        }

        @Test
        void omittedKeyAlwaysCreatesANewRow() {
            FakeVpsProvisioner provisioner = FakeVpsProvisioner.succeeding();
            VpsManager manager = manager(provisioner);

            manager.create(VpsSpec.builder("web-01", IMAGE).build());
            manager.create(VpsSpec.builder("web-02", IMAGE).build());

            assertThat(provisioner.createCallCount()).isEqualTo(2);
            assertThat(repository.findAll()).hasSize(2);
        }
    }

    @Nested
    class Concurrency {

        @Test
        void concurrentCreatesWithTheSameKeyProduceExactlyOneRow() throws InterruptedException {
            FakeVpsProvisioner provisioner = FakeVpsProvisioner.succeeding();
            VpsManager manager = manager(provisioner);
            VpsSpec spec = VpsSpec.builder("web-01", IMAGE).idempotencyKey("shared-key").build();
            int threads = 20;

            runConcurrently(threads, () -> manager.create(spec));

            assertThat(provisioner.createCallCount()).isEqualTo(1);
            assertThat(repository.findAll()).hasSize(1);
        }

        @Test
        void concurrentCreatesWithDistinctKeysProduceExactlyNRows() throws InterruptedException {
            FakeVpsProvisioner provisioner = FakeVpsProvisioner.succeeding();
            VpsManager manager = manager(provisioner);
            int threads = 20;
            AtomicInteger counter = new AtomicInteger();

            runConcurrently(threads, () -> manager.create(
                    VpsSpec.builder("web-" + counter.incrementAndGet(), IMAGE).build()));

            assertThat(provisioner.createCallCount()).isEqualTo(threads);
            assertThat(repository.findAll()).hasSize(threads);
        }

        private void runConcurrently(int threads, Runnable action) throws InterruptedException {
            CountDownLatch ready = new CountDownLatch(threads);
            CountDownLatch start = new CountDownLatch(1);
            // Pool size must equal `threads`: every task has to be running (able to reach
            // ready.countDown()) before any of them blocks on `start`, or queued-but-not-yet-
            // running tasks starve the ones ahead of them and ready.await() never completes.
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            try {
                List<Runnable> tasks = IntStream.range(0, threads).<Runnable>mapToObj(i -> () -> {
                    ready.countDown();
                    awaitUninterruptibly(start);
                    action.run();
                }).toList();
                tasks.forEach(pool::submit);
                ready.await();
                start.countDown();
            } finally {
                pool.shutdown();
                pool.awaitTermination(5, TimeUnit.SECONDS);
            }
        }

        private void awaitUninterruptibly(CountDownLatch latch) {
            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
