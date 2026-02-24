package com.eyarko.ecom.security;

import com.eyarko.ecom.entity.UserRole;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OAuth2RoleResolver {
    private final Set<String> adminEmails;

    public OAuth2RoleResolver(
        @Value("${app.security.oauth2.admin-emails:}") String adminEmails
    ) {
        this.adminEmails = parseEmails(adminEmails);
    }

    public UserRole resolveRole(String email) {
        if (email == null) {
            return UserRole.CUSTOMER;
        }
        String normalized = email.trim().toLowerCase();
        if (adminEmails.contains(normalized)) {
            return UserRole.ADMIN;
        }
        return UserRole.CUSTOMER;
    }

    private static Set<String> parseEmails(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(raw.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
    }
}


