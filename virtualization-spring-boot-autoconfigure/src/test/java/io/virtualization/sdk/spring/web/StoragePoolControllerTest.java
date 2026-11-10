package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.core.exception.ResourceNotFoundException;
import io.virtualization.sdk.spring.web.support.FakeStorageProvider;
import io.virtualization.sdk.storage.StoragePool;
import io.virtualization.sdk.storage.StorageProviderRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StoragePoolControllerTest {

    private StoragePoolController controller;

    @BeforeEach
    void setUp() {
        StorageProviderRegistry registry = new StorageProviderRegistry(Map.of("fake", new FakeStorageProvider()));
        controller = new StoragePoolController(registry);
    }

    @Test
    void listReturnsTheSeededDefaultPool() {
        List<StoragePool> pools = controller.list("fake");

        assertThat(pools).extracting(StoragePool::name).containsExactly("default");
    }

    @Test
    void getReturnsTheNamedPool() {
        StoragePool pool = controller.get("default", "fake");

        assertThat(pool.driver()).isEqualTo("dir");
    }

    @Test
    void getUnknownPoolThrowsResourceNotFound() {
        assertThatThrownBy(() -> controller.get("missing", "fake")).isInstanceOf(ResourceNotFoundException.class);
    }
}
