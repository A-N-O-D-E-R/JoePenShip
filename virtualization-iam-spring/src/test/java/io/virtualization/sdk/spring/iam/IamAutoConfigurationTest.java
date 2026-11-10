package io.virtualization.sdk.spring.iam;

import io.virtualization.sdk.iam.ApiCredentialManager;
import io.virtualization.sdk.iam.ApiCredentialRepository;
import io.virtualization.sdk.iam.AuthorizationService;
import io.virtualization.sdk.iam.Bootstrap;
import io.virtualization.sdk.iam.GroupRepository;
import io.virtualization.sdk.iam.RoleRepository;
import io.virtualization.sdk.iam.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.security.web.SecurityFilterChain;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class IamAutoConfigurationTest {

    private final AutoConfigurations autoConfigurations = AutoConfigurations.of(IamAutoConfiguration.class);

    @Test
    void disabledByDefaultRegistersNoBeans(@TempDir Path tempDir) {
        new WebApplicationContextRunner()
                .withConfiguration(autoConfigurations)
                .withPropertyValues("virtualization.iam.state-file=" + tempDir.resolve("iam.json"))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(AuthorizationService.class);
                    assertThat(context).doesNotHaveBean(SecurityFilterChain.class);
                });
    }

    @Test
    void enabledWiresEveryBean(@TempDir Path tempDir) {
        new WebApplicationContextRunner()
                .withConfiguration(autoConfigurations)
                .withPropertyValues(
                        "virtualization.iam.enabled=true",
                        "virtualization.iam.state-file=" + tempDir.resolve("iam.json"))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(UserRepository.class);
                    assertThat(context).hasSingleBean(RoleRepository.class);
                    assertThat(context).hasSingleBean(GroupRepository.class);
                    assertThat(context).hasSingleBean(ApiCredentialRepository.class);
                    assertThat(context).hasSingleBean(AuthorizationService.class);
                    assertThat(context).hasSingleBean(ApiCredentialManager.class);
                    assertThat(context).hasSingleBean(SecurityFilterChain.class);
                });
    }

    @Test
    void enabledBootstrapsADefaultAdminOnFirstRun(@TempDir Path tempDir) {
        new WebApplicationContextRunner()
                .withConfiguration(autoConfigurations)
                .withPropertyValues(
                        "virtualization.iam.enabled=true",
                        "virtualization.iam.state-file=" + tempDir.resolve("iam.json"))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    // ApplicationContextRunner/WebApplicationContextRunner don't go through
                    // SpringApplication.run(), so ApplicationRunner beans are registered but never
                    // auto-invoked the way a real application startup would — invoke it explicitly.
                    context.getBean(ApplicationRunner.class).run(new DefaultApplicationArguments());

                    UserRepository users = context.getBean(UserRepository.class);
                    assertThat(users.findByUsername(Bootstrap.DEFAULT_ADMIN_USERNAME)).isPresent();
                });
    }
}
