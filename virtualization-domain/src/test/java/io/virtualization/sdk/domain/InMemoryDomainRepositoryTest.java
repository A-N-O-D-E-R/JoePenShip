package io.virtualization.sdk.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryDomainRepositoryTest {

    private static Domain sampleDomain(String name) {
        return new Domain(DomainId.generate(), name, DomainStatus.ACTIVE, null, Instant.now());
    }

    @Test
    void saveAndFindById() {
        InMemoryDomainRepository repository = new InMemoryDomainRepository();
        Domain domain = sampleDomain("example.com");

        repository.save(domain);

        assertThat(repository.findById(domain.id())).contains(domain);
    }

    @Test
    void findByIdUnknownIsEmpty() {
        InMemoryDomainRepository repository = new InMemoryDomainRepository();

        assertThat(repository.findById(DomainId.generate())).isEmpty();
    }

    @Test
    void findAllReturnsEverySavedRow() {
        InMemoryDomainRepository repository = new InMemoryDomainRepository();
        Domain a = sampleDomain("example.com");
        Domain b = sampleDomain("example.net");

        repository.save(a);
        repository.save(b);

        assertThat(repository.findAll()).containsExactlyInAnyOrder(a, b);
    }

    @Test
    void deleteUnknownIdIsANoOp() {
        InMemoryDomainRepository repository = new InMemoryDomainRepository();

        repository.delete(DomainId.generate());

        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void deleteRemovesTheRow() {
        InMemoryDomainRepository repository = new InMemoryDomainRepository();
        Domain domain = sampleDomain("example.com");
        repository.save(domain);

        repository.delete(domain.id());

        assertThat(repository.findById(domain.id())).isEmpty();
    }

    @Test
    void concurrentSavesAreAllVisible() throws InterruptedException {
        InMemoryDomainRepository repository = new InMemoryDomainRepository();
        int count = 50;
        List<Domain> rows = java.util.stream.IntStream.range(0, count).mapToObj(i -> sampleDomain("example" + i + ".com")).toList();
        CountDownLatch ready = new CountDownLatch(count);
        CountDownLatch start = new CountDownLatch(1);
        // Pool size must equal the party count: every task has to be running (able to reach
        // ready.countDown()) before any of them blocks on `start`, or the queued-but-not-yet-
        // running tasks starve the ones ahead of them and ready.await() never completes.
        ExecutorService pool = Executors.newFixedThreadPool(count);
        try {
            for (Domain row : rows) {
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
