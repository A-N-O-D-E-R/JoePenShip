package io.virtualization.sdk.certificate;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryCertificateRepositoryTest {

    private static Certificate sampleCertificate(String domain) {
        return new Certificate(CertificateId.generate(), CertificateStatus.REQUESTED, List.of(domain), null, null, "letsencrypt");
    }

    @Test
    void saveAndFindById() {
        InMemoryCertificateRepository repository = new InMemoryCertificateRepository();
        Certificate certificate = sampleCertificate("example.com");

        repository.save(certificate);

        assertThat(repository.findById(certificate.id())).contains(certificate);
    }

    @Test
    void findByIdUnknownIsEmpty() {
        InMemoryCertificateRepository repository = new InMemoryCertificateRepository();

        assertThat(repository.findById(CertificateId.generate())).isEmpty();
    }

    @Test
    void findAllReturnsEverySavedRow() {
        InMemoryCertificateRepository repository = new InMemoryCertificateRepository();
        Certificate a = sampleCertificate("example.com");
        Certificate b = sampleCertificate("example.net");

        repository.save(a);
        repository.save(b);

        assertThat(repository.findAll()).containsExactlyInAnyOrder(a, b);
    }

    @Test
    void deleteUnknownIdIsANoOp() {
        InMemoryCertificateRepository repository = new InMemoryCertificateRepository();

        repository.delete(CertificateId.generate());

        assertThat(repository.findAll()).isEmpty();
    }

    @Test
    void deleteRemovesTheRow() {
        InMemoryCertificateRepository repository = new InMemoryCertificateRepository();
        Certificate certificate = sampleCertificate("example.com");
        repository.save(certificate);

        repository.delete(certificate.id());

        assertThat(repository.findById(certificate.id())).isEmpty();
    }

    @Test
    void concurrentSavesAreAllVisible() throws InterruptedException {
        InMemoryCertificateRepository repository = new InMemoryCertificateRepository();
        int count = 50;
        List<Certificate> rows = java.util.stream.IntStream.range(0, count).mapToObj(i -> sampleCertificate("example" + i + ".com")).toList();
        CountDownLatch ready = new CountDownLatch(count);
        CountDownLatch start = new CountDownLatch(1);
        // Pool size must equal the party count: every task has to be running (able to reach
        // ready.countDown()) before any of them blocks on `start`, or the queued-but-not-yet-
        // running tasks starve the ones ahead of them and ready.await() never completes.
        ExecutorService pool = Executors.newFixedThreadPool(count);
        try {
            for (Certificate row : rows) {
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
