package io.virtualization.sdk.vps;

import io.virtualization.sdk.core.ComputeResources;
import io.virtualization.sdk.core.image.ImageReference;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryVpsRepositoryTest {

    private static final ImageReference IMAGE = new ImageReference("incus", "images", "ubuntu/24.04");

    private static Vps sampleVps(String name) {
        Instant now = Instant.now();
        return new Vps(
                VpsId.generate(), name, VpsState.READY, VpsType.VIRTUAL_MACHINE, IMAGE, new ComputeResources(1, 1_024),
                new StorageConfiguration(DataSize.ofGigabytes(10)), NetworkConfiguration.UNSPECIFIED,
                VpsSpec.builder(name, IMAGE).build(), null, null, null, now, now, null, null, null);
    }

    @Test
    void saveAndFindById() {
        InMemoryVpsRepository repository = new InMemoryVpsRepository();
        Vps vps = sampleVps("web-01");

        repository.save(vps);

        assertThat(repository.findById(vps.id())).contains(vps);
    }

    @Test
    void findByIdUnknownIsEmpty() {
        InMemoryVpsRepository repository = new InMemoryVpsRepository();

        assertThat(repository.findById(VpsId.generate())).isEmpty();
    }

    @Test
    void findAllReturnsEverySavedRow() {
        InMemoryVpsRepository repository = new InMemoryVpsRepository();
        Vps a = sampleVps("web-01");
        Vps b = sampleVps("web-02");

        repository.save(a);
        repository.save(b);

        assertThat(repository.findAll()).containsExactlyInAnyOrder(a, b);
    }

    @Test
    void deleteUnknownIdIsANoOp() {
        InMemoryVpsRepository repository = new InMemoryVpsRepository();

        repository.delete(VpsId.generate());

        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void deleteRemovesTheRow() {
        InMemoryVpsRepository repository = new InMemoryVpsRepository();
        Vps vps = sampleVps("web-01");
        repository.save(vps);

        repository.delete(vps.id());

        assertThat(repository.findById(vps.id())).isEmpty();
    }

    @Test
    void concurrentSavesAreAllVisible() throws InterruptedException {
        InMemoryVpsRepository repository = new InMemoryVpsRepository();
        int count = 50;
        List<Vps> rows = java.util.stream.IntStream.range(0, count).mapToObj(i -> sampleVps("web-" + i)).toList();
        CountDownLatch ready = new CountDownLatch(count);
        CountDownLatch start = new CountDownLatch(1);
        // Pool size must equal the party count: every task has to be running (able to reach
        // ready.countDown()) before any of them blocks on `start`, or the queued-but-not-yet-
        // running tasks starve the ones ahead of them and ready.await() never completes.
        ExecutorService pool = Executors.newFixedThreadPool(count);
        try {
            for (Vps row : rows) {
                pool.submit(() -> {
                    ready.countDown();
                    await(start);
                    repository.save(row);
                });
            }
            ready.await();
            start.countDown();
        } finally {
            pool.shutdown();
            pool.awaitTermination(5, TimeUnit.SECONDS);
        }

        assertThat(repository.findAll()).hasSize(count);
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
