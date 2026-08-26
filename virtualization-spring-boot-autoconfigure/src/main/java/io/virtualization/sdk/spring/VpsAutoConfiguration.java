package io.virtualization.sdk.spring;

import io.virtualization.sdk.core.VirtualizationClient;
import io.virtualization.sdk.vps.DefaultVpsManager;
import io.virtualization.sdk.vps.DefaultVpsProvisioner;
import io.virtualization.sdk.vps.InMemoryVpsRepository;
import io.virtualization.sdk.vps.VpsManager;
import io.virtualization.sdk.vps.VpsProvisioner;
import io.virtualization.sdk.vps.VpsRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Wires a {@link VpsManager} on top of the {@link VirtualizationClient}'s existing
 * provider/image-provider registries — active only when {@code virtualization.vps.provider}
 * names one of the entries already configured under {@code virtualization.providers} (see {@link
 * VirtualizationAutoConfiguration}). Absent that property, no {@link VpsManager} bean is created
 * and {@code VpsController} (conditional on this bean) never registers.
 *
 * <p>{@link InMemoryVpsRepository} only — a deployment that needs persistence supplies its own
 * {@link VpsRepository} bean, which this backs off for via {@code @ConditionalOnMissingBean}.
 */
@AutoConfiguration(after = VirtualizationAutoConfiguration.class)
@EnableConfigurationProperties(VpsProperties.class)
@ConditionalOnProperty(prefix = "virtualization.vps", name = "provider")
public class VpsAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public VpsRepository vpsRepository() {
        return new InMemoryVpsRepository();
    }

    @Bean
    @ConditionalOnMissingBean
    public VpsManager vpsManager(VirtualizationClient client, VpsProperties properties, VpsRepository repository) {
        VpsProvisioner provisioner =
                new DefaultVpsProvisioner(client.provider(properties.provider()), client.images(properties.provider()));
        return new DefaultVpsManager(repository, provisioner);
    }
}
