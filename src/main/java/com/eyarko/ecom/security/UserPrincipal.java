package com.eyarko.ecom.security;

import com.eyarko.ecom.entity.User;
import java.util.Collection;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
@Builder
public class UserPrincipal implements UserDetails {
    private final Long id;
    private final String fullName;
    private final String email;
    private final String passwordHash;
    private final List<GrantedAuthority> authorities;

    public static UserPrincipal fromUser(User user) {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        return UserPrincipal.builder()
            .id(user.getId())
            .fullName(user.getFullName())
            .email(user.getEmail())
            .passwordHash(user.getPasswordHash())
            .authorities(authorities)
            .build();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
