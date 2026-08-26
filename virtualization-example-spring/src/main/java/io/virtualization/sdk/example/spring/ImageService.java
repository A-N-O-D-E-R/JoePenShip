package io.virtualization.sdk.example.spring;

import io.virtualization.sdk.core.VirtualizationClient;
import io.virtualization.sdk.core.image.Image;
import io.virtualization.sdk.core.image.ImageQuery;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Demonstrates injecting the auto-configured image side of the SDK, independently of {@link
 * VmService}'s workload side — same {@link VirtualizationClient} bean, different accessor
 * ({@link VirtualizationClient#images} rather than {@link VirtualizationClient#provider}).
 */
@Service
public class ImageService {

    private final VirtualizationClient client;

    public ImageService(VirtualizationClient client) {
        this.client = client;
    }

    public List<Image> list(String providerName) {
        return client.images(providerName).list();
    }

    public List<Image> search(String providerName, String distribution) {
        return client.images(providerName).search(ImageQuery.builder().distribution(distribution).build());
    }
}
