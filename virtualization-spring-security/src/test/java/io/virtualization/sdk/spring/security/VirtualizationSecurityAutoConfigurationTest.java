package io.virtualization.sdk.spring.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VirtualizationSecurityAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(VirtualizationSecurityAutoConfiguration.class));

    @Test
    void noGrantedAuthoritiesMapperWithoutAClaimsToRoleMapperBean() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(GrantedAuthoritiesMapper.class);
        });
    }

    @Test
    void wiresGrantedAuthoritiesMapperWhenClaimsToRoleMapperBeanExists() {
        contextRunner.withUserConfiguration(ClaimsMapperConfig.class).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(GrantedAuthoritiesMapper.class);
            assertThat(context.getBean(GrantedAuthoritiesMapper.class))
                    .isInstanceOf(VirtualizationGrantedAuthoritiesMapper.class);
        });
    }

    @Test
    void userDefinedGrantedAuthoritiesMapperIsNotOverridden() {
        contextRunner.withUserConfiguration(ClaimsMapperConfig.class, CustomMapperConfig.class).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(GrantedAuthoritiesMapper.class);
            assertThat(context.getBean(GrantedAuthoritiesMapper.class)).isSameAs(CustomMapperConfig.CUSTOM_MAPPER);
        });
    }

    @Configuration
    static class ClaimsMapperConfig {
        @Bean
        ClaimsToRoleMapper claimsToRoleMapper() {
            return claims -> List.of("ROLE_ADMIN");
        }
    }

    @Configuration
    static class CustomMapperConfig {
        static final GrantedAuthoritiesMapper CUSTOM_MAPPER = authorities -> authorities;

        @Bean
        GrantedAuthoritiesMapper grantedAuthoritiesMapper() {
            return CUSTOM_MAPPER;
        }
    }
}
