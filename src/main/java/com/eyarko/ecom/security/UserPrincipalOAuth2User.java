package com.eyarko.ecom.security;

import java.util.Collection;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * Bridges Spring Security OAuth2User with the application's {@link UserPrincipal}.
 * <p>
 * This lets OAuth2-authenticated requests reuse the same principal/authorities model as JWT auth.
 */
public class UserPrincipalOAuth2User implements OAuth2User {
    private final UserPrincipal principal;
    private final Map<String, Object> attributes;

    public UserPrincipalOAuth2User(UserPrincipal principal, Map<String, Object> attributes) {
        this.principal = principal;
        this.attributes = attributes;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return principal.getAuthorities();
    }

    @Override
    public String getName() {
        return principal.getUsername();
    }

    public UserPrincipal getPrincipal() {
        return principal;
    }
}


