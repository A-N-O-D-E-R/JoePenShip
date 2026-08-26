package io.virtualization.sdk.spring;

import io.virtualization.sdk.core.ProviderRegistry;
import io.virtualization.sdk.core.VirtualizationClient;
import io.virtualization.sdk.core.VirtualizationProvider;
import io.virtualization.sdk.core.image.ImageProvider;
import io.virtualization.sdk.core.image.ImageProviderRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Wires a {@link ProviderRegistry}/{@link ImageProviderRegistry} pair and a {@link
 * VirtualizationClient} from {@code virtualization.providers.*} configuration.
 *
 * <p>All beans are {@code @ConditionalOnMissingBean}: an application that defines its own {@link
 * ProviderRegistry}, {@link ImageProviderRegistry} or {@link VirtualizationClient} bean (e.g. to
 * build providers programmatically) is never overridden by this auto-configuration.
 */
@AutoConfiguration
@EnableConfigurationProperties(VirtualizationProperties.class)
public class VirtualizationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ProviderRegistry providerRegistry(VirtualizationProperties properties) {
        Map<String, VirtualizationProvider> providers = new LinkedHashMap<>();
        properties.providers().forEach((name, entry) -> providers.put(name, ProviderFactory.create(name, entry)));
        return new ProviderRegistry(providers);
    }

    @Bean
    @ConditionalOnMissingBean
    public ImageProviderRegistry imageProviderRegistry(VirtualizationProperties properties) {
        Map<String, ImageProvider> images = new LinkedHashMap<>();
        properties.providers()
                .forEach((name, entry) -> ProviderFactory.createImageProvider(name, entry).ifPresent(p -> images.put(name, p)));
        return new ImageProviderRegistry(images);
    }

    @Bean
    @ConditionalOnMissingBean
    public VirtualizationClient virtualizationClient(ProviderRegistry providerRegistry, ImageProviderRegistry imageProviderRegistry) {
        return new VirtualizationClient(providerRegistry, imageProviderRegistry);
    }
}
