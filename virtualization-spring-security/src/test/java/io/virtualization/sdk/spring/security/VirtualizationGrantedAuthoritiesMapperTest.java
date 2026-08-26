package io.virtualization.sdk.spring.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class VirtualizationGrantedAuthoritiesMapperTest {

    private static OidcUserAuthority oidcAuthority(Map<String, Object> claims) {
        OidcIdToken idToken =
                OidcIdToken.withTokenValue("token").issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).claims(c -> c.putAll(claims)).build();
        OidcUserInfo userInfo = new OidcUserInfo(claims);
        return new OidcUserAuthority(idToken, userInfo);
    }

    @Test
    void mapsOidcAuthorityClaimsToAdditionalRoles() {
        VirtualizationGrantedAuthoritiesMapper mapper =
                new VirtualizationGrantedAuthoritiesMapper(claims -> List.of("ROLE_ADMIN"));
        OidcUserAuthority authority = oidcAuthority(Map.of("sub", "user-1"));

        Collection<GrantedAuthority> mapped = new ArrayList<>(mapper.mapAuthorities(List.of(authority)));

        assertThat(mapped).contains(authority);
        assertThat(mapped).extracting(GrantedAuthority::getAuthority).contains("ROLE_ADMIN");
    }

    @Test
    void nonOidcAuthoritiesPassThroughUnchanged() {
        VirtualizationGrantedAuthoritiesMapper mapper = new VirtualizationGrantedAuthoritiesMapper(claims -> List.of());
        SimpleGrantedAuthority plain = new SimpleGrantedAuthority("SCOPE_openid");

        Collection<GrantedAuthority> mapped = new ArrayList<>(mapper.mapAuthorities(List.of(plain)));

        assertThat(mapped).containsExactly(plain);
    }

    @Test
    void emptyRoleMappingAddsNothingExtra() {
        VirtualizationGrantedAuthoritiesMapper mapper = new VirtualizationGrantedAuthoritiesMapper(claims -> List.of());
        OidcUserAuthority authority = oidcAuthority(Map.of("sub", "user-1"));

        Collection<GrantedAuthority> mapped = new ArrayList<>(mapper.mapAuthorities(List.of(authority)));

        assertThat(mapped).containsExactly(authority);
    }
}
