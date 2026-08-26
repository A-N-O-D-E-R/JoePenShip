package io.virtualization.sdk.spring.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Adapts a {@link ClaimsToRoleMapper} into Spring Security's {@link GrantedAuthoritiesMapper}
 * contract: for each {@link OidcUserAuthority} among the authenticated user's authorities, maps
 * its ID-token claims to role names and adds them as {@link SimpleGrantedAuthority}s. Non-OIDC
 * authorities and the original OIDC scope authorities pass through unchanged.
 */
public final class VirtualizationGrantedAuthoritiesMapper implements GrantedAuthoritiesMapper {

    private final ClaimsToRoleMapper claimsToRoleMapper;

    public VirtualizationGrantedAuthoritiesMapper(ClaimsToRoleMapper claimsToRoleMapper) {
        this.claimsToRoleMapper = Objects.requireNonNull(claimsToRoleMapper, "claimsToRoleMapper must not be null");
    }

    @Override
    public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
        Set<GrantedAuthority> mapped = new LinkedHashSet<>(authorities);
        for (GrantedAuthority authority : authorities) {
            if (authority instanceof OidcUserAuthority oidcAuthority) {
                claimsToRoleMapper.map(oidcAuthority.getUserInfo().getClaims())
                        .forEach(role -> mapped.add(new SimpleGrantedAuthority(role)));
            }
        }
        return mapped;
    }
}
