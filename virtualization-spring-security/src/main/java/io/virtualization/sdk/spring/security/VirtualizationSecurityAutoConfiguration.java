package io.virtualization.sdk.spring.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

/**
 * Bridges an application-supplied {@link ClaimsToRoleMapper} into Spring Security's OIDC login
 * flow. Activates only when Spring Security's OAuth2 client is on the classpath ({@code
 * ClientRegistrationRepository} — brought in by {@code spring-boot-starter-security-oauth2-client}
 * or an equivalent, e.g. Okta's own starter, since Okta is a standards-compliant OIDC provider and
 * needs no Okta-specific dependency here).
 *
 * <p><b>Scope</b>: this secures the application's own HTTP endpoints. It has no knowledge of, and
 * never touches, {@code ProxmoxCredentials}, {@code IncusTlsCredentials}, or any other provider
 * credential — those remain configured independently via {@code virtualization.providers.*} (see
 * {@code virtualization-spring-boot-autoconfigure}), regardless of which application user is
 * logged in. An authenticated user's OIDC access token is never forwarded to Proxmox, Incus or
 * QEMU.
 *
 * <p><b>Extending to other identity providers</b>: Kerberos/SPNEGO or any other Spring-Security-
 * supported mechanism plugs in the same way this module does — as its own {@code
 * AuthenticationProvider}/{@code SecurityFilterChain}/{@code GrantedAuthoritiesMapper} beans in
 * the consuming application, requiring no change to {@code virtualization-core} or the provider
 * modules. This module does not bundle Kerberos support itself: it pulls in no
 * not-yet-needed dependency for it.
 *
 * <p>Deliberately does not define a default {@code SecurityFilterChain} — an application's
 * authorization requirements (which endpoints are public, CORS, actuator exposure, multiple
 * filter chains) are too specific to guess at; Spring Security's own auto-configuration already
 * provides a reasonable default once OAuth2 client properties are configured, and the application
 * remains free to define its own.
 */
@AutoConfiguration
@ConditionalOnClass(ClientRegistrationRepository.class)
public class VirtualizationSecurityAutoConfiguration {

    @Bean
    @ConditionalOnBean(ClaimsToRoleMapper.class)
    @ConditionalOnMissingBean
    public GrantedAuthoritiesMapper virtualizationGrantedAuthoritiesMapper(ClaimsToRoleMapper claimsToRoleMapper) {
        return new VirtualizationGrantedAuthoritiesMapper(claimsToRoleMapper);
    }
}
