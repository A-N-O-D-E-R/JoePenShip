package io.virtualization.sdk.certificate;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryCertificateStoreTest {

    private static final CertificateMaterial MATERIAL = new CertificateMaterial("cert-pem", "key-pem", "chain-pem");

    @Test
    void storeAndLoad() {
        InMemoryCertificateStore store = new InMemoryCertificateStore();
        CertificateId id = CertificateId.generate();

        store.store(id, MATERIAL);

        assertThat(store.load(id)).contains(MATERIAL);
    }

    @Test
    void loadUnknownIdIsEmpty() {
        InMemoryCertificateStore store = new InMemoryCertificateStore();

        assertThat(store.load(CertificateId.generate())).isEmpty();
    }

    @Test
    void deleteRemovesTheMaterial() {
        InMemoryCertificateStore store = new InMemoryCertificateStore();
        CertificateId id = CertificateId.generate();
        store.store(id, MATERIAL);

        store.delete(id);

        assertThat(store.load(id)).isEmpty();
    }

    @Test
    void deleteUnknownIdIsANoOp() {
        InMemoryCertificateStore store = new InMemoryCertificateStore();

        store.delete(CertificateId.generate());
    }
}
