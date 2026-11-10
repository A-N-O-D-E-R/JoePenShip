package io.virtualization.sdk.spring.web;

import io.virtualization.sdk.storage.StoragePool;
import io.virtualization.sdk.storage.StorageProviderRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * {@code /api/v1/storage/pools} — read-only, over {@link StorageProviderRegistry}. {@code
 * provider} selects which configured entry to use, the same role {@code --provider} plays in the
 * CLI and {@code provider} plays in {@link ImageController}.
 */
@RestController
@RequestMapping("/api/v1/storage/pools")
public class StoragePoolController {

    private final StorageProviderRegistry registry;

    public StoragePoolController(StorageProviderRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    public List<StoragePool> list(@RequestParam String provider) {
        return registry.get(provider).listStoragePools();
    }

    @GetMapping("/{pool}")
    public StoragePool get(@PathVariable String pool, @RequestParam String provider) {
        return registry.get(provider).getStoragePool(pool);
    }
}
