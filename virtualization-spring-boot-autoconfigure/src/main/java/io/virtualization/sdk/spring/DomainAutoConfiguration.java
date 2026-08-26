package io.virtualization.sdk.spring;

import io.virtualization.sdk.domain.DefaultDomainManager;
import io.virtualization.sdk.domain.DomainManager;
import io.virtualization.sdk.domain.DomainRepository;
import io.virtualization.sdk.domain.InMemoryDomainRepository;
import io.virtualization.sdk.dns.DnsProviderRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Wires a {@link DomainManager} on top of {@link DnsAutoConfiguration}'s {@link
 * DnsProviderRegistry} — active only when {@code virtualization.domains.enabled=true}. Absent
 * that, no {@link DomainManager} bean is created and {@code DomainController} (conditional on this
 * bean) never registers.
 *
 * <p>{@link InMemoryDomainRepository} only — a deployment that needs persistence supplies its own
 * {@link DomainRepository} bean, which this backs off for via {@code @ConditionalOnMissingBean}.
 */
@AutoConfiguration(after = DnsAutoConfiguration.class)
@EnableConfigurationProperties(DomainProperties.class)
@ConditionalOnProperty(prefix = "virtualization.domains", name = "enabled", havingValue = "true")
public class DomainAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DomainRepository domainRepository() {
        return new InMemoryDomainRepository();
    }

    @Bean
    @ConditionalOnMissingBean
    public DomainManager domainManager(DomainRepository repository, DnsProviderRegistry dnsProviders) {
        return new DefaultDomainManager(repository, dnsProviders);
    }
}
