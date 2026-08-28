package io.virtualization.sdk.spring.iam;

import io.virtualization.sdk.iam.Role;
import io.virtualization.sdk.iam.RoleRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Expands whatever authorities the current request's {@link Authentication} already carries with
 * the {@code virtualization-iam} permissions of any {@link Role} whose name matches one of them —
 * e.g. an OIDC login that already resolved a {@code ROLE_operators} authority (via an
 * application-supplied {@code ClaimsToRoleMapper} in the optional {@code
 * virtualization-spring-security} module, or any other mechanism) additionally gets that role's
 * {@code vps:create}-shaped permissions, the same ones {@link PermissionRequestMatchers} checks.
 *
 * <p>Deliberately filter-based rather than a {@code GrantedAuthoritiesMapper} decorator: this
 * module has no compile dependency on {@code virtualization-spring-security} (or vice versa), and
 * reading/replacing the already-resolved {@link Authentication} here works uniformly for *any*
 * upstream authentication mechanism (OIDC, this module's own API tokens, or a future one) without
 * needing to locate or re-wrap a specific prior bean of the same type — {@link
 * SecurityContextHolder}'s context is already populated by the time this filter runs, regardless
 * of how.
 */
final class PermissionExpandingAuthenticationFilter extends OncePerRequestFilter {

    private static final String ROLE_PREFIX = "ROLE_";

    private final RoleRepository roles;

    PermissionExpandingAuthenticationFilter(RoleRepository roles) {
        this.roles = roles;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication current = SecurityContextHolder.getContext().getAuthentication();
        if (current != null && current.isAuthenticated()) {
            Set<GrantedAuthority> expanded = new LinkedHashSet<>(current.getAuthorities());
            for (GrantedAuthority authority : current.getAuthorities()) {
                String name = stripRolePrefix(authority.getAuthority());
                roles.findByName(name)
                        .map(Role::permissions)
                        .ifPresent(permissions -> permissions.forEach(p -> expanded.add(new SimpleGrantedAuthority(p.asString()))));
            }
            if (expanded.size() > current.getAuthorities().size()) {
                SecurityContextHolder.getContext().setAuthentication(new ExpandedAuthentication(current, expanded));
            }
        }
        chain.doFilter(request, response);
    }

    private static String stripRolePrefix(String authority) {
        return authority.startsWith(ROLE_PREFIX) ? authority.substring(ROLE_PREFIX.length()) : authority;
    }

    private static final class ExpandedAuthentication extends AbstractAuthenticationToken {

        private final Authentication original;

        ExpandedAuthentication(Authentication original, Set<GrantedAuthority> expandedAuthorities) {
            super(expandedAuthorities);
            this.original = original;
            setAuthenticated(true);
        }

        @Override
        public Object getCredentials() {
            return original.getCredentials();
        }

        @Override
        public Object getPrincipal() {
            return original.getPrincipal();
        }
    }
}
