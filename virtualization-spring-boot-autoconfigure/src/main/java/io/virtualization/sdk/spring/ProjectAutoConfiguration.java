package io.virtualization.sdk.spring;

import io.virtualization.sdk.project.DefaultProjectManager;
import io.virtualization.sdk.project.InMemoryProjectRepository;
import io.virtualization.sdk.project.ProjectManager;
import io.virtualization.sdk.project.ProjectRepository;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Wires a {@link ProjectManager} — always created, the same stance {@link
 * VirtualizationAutoConfiguration} takes for {@code ProviderRegistry}/{@code
 * ImageProviderRegistry}: a project needs no upstream provider configuration to make sense, unlike
 * VPS/Domain/Certificate, so there's no {@code virtualization.projects.enabled} flag to gate on.
 *
 * <p>{@link InMemoryProjectRepository} only — a deployment that needs persistence supplies its own
 * {@link ProjectRepository} bean, which this backs off for via {@code @ConditionalOnMissingBean}.
 */
@AutoConfiguration
public class ProjectAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ProjectRepository projectRepository() {
        return new InMemoryProjectRepository();
    }

    @Bean
    @ConditionalOnMissingBean
    public ProjectManager projectManager(ProjectRepository repository) {
        return new DefaultProjectManager(repository);
    }
}
