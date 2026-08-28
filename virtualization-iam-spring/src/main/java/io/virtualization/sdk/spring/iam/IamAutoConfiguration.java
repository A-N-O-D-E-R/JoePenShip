package io.virtualization.sdk.spring.iam;

import io.virtualization.sdk.iam.ApiCredentialManager;
import io.virtualization.sdk.iam.ApiCredentialRepository;
import io.virtualization.sdk.iam.AuthorizationService;
import io.virtualization.sdk.iam.Bootstrap;
import io.virtualization.sdk.iam.DefaultApiCredentialManager;
import io.virtualization.sdk.iam.DefaultAuthorizationService;
import io.virtualization.sdk.iam.DefaultGroupManager;
import io.virtualization.sdk.iam.DefaultRoleManager;
import io.virtualization.sdk.iam.DefaultUserManager;
import io.virtualization.sdk.iam.GroupManager;
import io.virtualization.sdk.iam.GroupRepository;
import io.virtualization.sdk.iam.RoleManager;
import io.virtualization.sdk.iam.RoleRepository;
import io.virtualization.sdk.iam.UserManager;
import io.virtualization.sdk.iam.UserRepository;
import io.virtualization.sdk.iam.store.JsonFileIamStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;

import java.nio.file.Path;

/**
 * Wires a {@link JsonFileIamStore} into the four {@code virtualization-iam} repositories, an
 * {@link AuthorizationService}, an {@link ApiCredentialManager}, and a {@link SecurityFilterChain}
 * gating {@code /api/v1/**} by permission (see {@link PermissionRequestMatchers}) — active only
 * when {@code virtualization.iam.enabled=true}, the same opt-in stance {@code
 * DomainAutoConfiguration}/{@code CertificateAutoConfiguration} take in {@code
 * virtualization-spring-boot-autoconfigure}. A peer of that module, not a dependency of it or vice
 * versa: this class never references its controllers, and gates them purely by URL pattern.
 */
@AutoConfiguration
@EnableWebSecurity
@EnableConfigurationProperties(IamProperties.class)
@ConditionalOnProperty(prefix = "virtualization.iam", name = "enabled", havingValue = "true")
public class IamAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(IamAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean
    public JsonFileIamStore iamStore(IamProperties properties) {
        Path stateFile = properties.stateFile() != null
                ? Path.of(properties.stateFile())
                : Path.of(System.getProperty("user.home"), ".virtualization", "iam.json");
        return new JsonFileIamStore(stateFile);
    }

    @Bean
    @ConditionalOnMissingBean
    public UserRepository userRepository(JsonFileIamStore store) {
        return store.users();
    }

    @Bean
    @ConditionalOnMissingBean
    public RoleRepository roleRepository(JsonFileIamStore store) {
        return store.roles();
    }

    @Bean
    @ConditionalOnMissingBean
    public GroupRepository groupRepository(JsonFileIamStore store) {
        return store.groups();
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiCredentialRepository apiCredentialRepository(JsonFileIamStore store) {
        return store.apiCredentials();
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthorizationService authorizationService(RoleRepository roles, GroupRepository groups) {
        return new DefaultAuthorizationService(roles, groups);
    }

    @Bean
    @ConditionalOnMissingBean
    public ApiCredentialManager apiCredentialManager(ApiCredentialRepository credentials, UserRepository users) {
        return new DefaultApiCredentialManager(credentials, users);
    }

    @Bean
    @ConditionalOnMissingBean
    public UserManager userManager(UserRepository users) {
        return new DefaultUserManager(users);
    }

    @Bean
    @ConditionalOnMissingBean
    public RoleManager roleManager(RoleRepository roles) {
        return new DefaultRoleManager(roles);
    }

    @Bean
    @ConditionalOnMissingBean
    public GroupManager groupManager(GroupRepository groups, UserRepository users) {
        return new DefaultGroupManager(groups, users);
    }

    /** Bootstraps a default admin (logged, not persisted in the clear) the first time the store is found empty. */
    @Bean
    public ApplicationRunner iamBootstrapRunner(UserRepository users, RoleRepository roles, ApiCredentialRepository apiCredentials) {
        return args -> Bootstrap.ensureAdmin(users, roles, apiCredentials).ifPresent(result -> log.warn(
                "Created default admin user '{}'. Password: {} API token: {} — save these now, they will not be shown again.",
                Bootstrap.DEFAULT_ADMIN_USERNAME, result.password(), result.apiToken()));
    }

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain iamSecurityFilterChain(
            HttpSecurity http, ApiCredentialManager apiCredentials, AuthorizationService authorizationService, RoleRepository roles)
            throws Exception {
        http.securityMatcher("/api/v1/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Without an explicit entry point, Spring Security's default for a pure
                // API-token setup (no httpBasic/formLogin configured) is 403 even for a
                // completely unauthenticated request — this restores the conventional
                // 401 (not authenticated) vs 403 (authenticated, wrong permission) split.
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(
                        (request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                .addFilterBefore(new ApiTokenAuthenticationFilter(apiCredentials, authorizationService), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new PermissionExpandingAuthenticationFilter(roles), ApiTokenAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> {
                    PermissionRequestMatchers.ENTRIES.forEach(
                            entry -> auth.requestMatchers(entry.method(), entry.pattern()).hasAuthority(entry.permission()));
                    auth.anyRequest().authenticated();
                });
        return http.build();
    }
}
