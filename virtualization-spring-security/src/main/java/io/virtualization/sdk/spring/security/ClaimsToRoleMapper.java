package io.virtualization.sdk.spring.security;

import java.util.Collection;
import java.util.Map;

/**
 * Maps authenticated OIDC claims to Spring Security role names.
 *
 * <p>How an identity provider's claims translate to application roles is inherently
 * application-specific (which claim holds the role/group, what the values mean) — this SDK
 * ships no default mapping. Provide your own implementation as a Spring bean and {@link
 * VirtualizationSecurityAutoConfiguration} wires it into a {@code GrantedAuthoritiesMapper}
 * automatically.
 */
@FunctionalInterface
public interface ClaimsToRoleMapper {

    /**
     * @param claims the OIDC claims from the ID token / userinfo response
     * @return role names (e.g. {@code "ROLE_ADMIN"}); empty if none apply
     */
    Collection<String> map(Map<String, Object> claims);
}
