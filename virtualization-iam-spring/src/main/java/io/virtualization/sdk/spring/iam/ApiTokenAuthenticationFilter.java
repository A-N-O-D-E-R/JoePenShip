package io.virtualization.sdk.spring.iam;

import io.virtualization.sdk.iam.ApiCredentialManager;
import io.virtualization.sdk.iam.AuthorizationService;
import io.virtualization.sdk.iam.Permission;
import io.virtualization.sdk.iam.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Authenticates {@code Authorization: Bearer <token>} against {@link ApiCredentialManager},
 * granting the resolved user's {@link AuthorizationService#effectivePermissions} as Spring
 * authorities — so {@code hasAuthority("vps:create")} in {@link PermissionRequestMatchers} just
 * works natively, no custom {@code AuthorizationManager}. No header, an unknown token, or an
 * expired one all leave the request unauthenticated rather than rejecting it directly here —
 * {@code authorizeHttpRequests} downstream rejects with 401 (no authentication) or 403 (wrong
 * permission) as appropriate, the same distinction {@code RestExceptionHandler} already draws for
 * {@code AuthenticationException}/{@code AuthorizationException} elsewhere in this SDK.
 */
final class ApiTokenAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final ApiCredentialManager apiCredentials;
    private final AuthorizationService authorizationService;

    ApiTokenAuthenticationFilter(ApiCredentialManager apiCredentials, AuthorizationService authorizationService) {
        this.apiCredentials = apiCredentials;
        this.authorizationService = authorizationService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            try {
                User user = apiCredentials.authenticate(header.substring(BEARER_PREFIX.length()));
                Set<Permission> permissions = authorizationService.effectivePermissions(user);
                List<GrantedAuthority> authorities = permissions.stream()
                        .<GrantedAuthority>map(p -> new SimpleGrantedAuthority(p.asString()))
                        .toList();
                SecurityContextHolder.getContext().setAuthentication(new ApiTokenAuthentication(user, authorities));
            } catch (io.virtualization.sdk.core.exception.AuthenticationException e) {
                // leave unauthenticated
            }
        }
        chain.doFilter(request, response);
    }

    private static final class ApiTokenAuthentication extends AbstractAuthenticationToken {

        private final User principal;

        ApiTokenAuthentication(User principal, List<GrantedAuthority> authorities) {
            super(authorities);
            this.principal = principal;
            setAuthenticated(true);
        }

        @Override
        public Object getCredentials() {
            return "";
        }

        @Override
        public Object getPrincipal() {
            return principal;
        }
    }
}
