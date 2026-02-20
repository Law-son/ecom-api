package com.eyarko.ecom.security;

import com.eyarko.ecom.entity.UserRole;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Resolves application roles for OAuth2-authenticated users.
 * <p>
 * This uses simple email allowlists configured via properties:
 * <ul>
 *   <li>{@code app.security.oauth2.admin-emails}</li>
 *   <li>{@code app.security.oauth2.staff-emails}</li>
 * </ul>
 * If an email is present in both lists, ADMIN wins.
 */
@Component
public class OAuth2RoleResolver {
    private final Set<String> adminEmails;
    private final Set<String> staffEmails;

    public OAuth2RoleResolver(
        @Value("${app.security.oauth2.admin-emails:}") String adminEmails,
        @Value("${app.security.oauth2.staff-emails:}") String staffEmails
    ) {
        this.adminEmails = parseEmails(adminEmails);
        this.staffEmails = parseEmails(staffEmails);
    }

    public UserRole resolveRole(String email) {
        if (email == null) {
            return UserRole.CUSTOMER;
        }
        String normalized = email.trim().toLowerCase();
        if (adminEmails.contains(normalized)) {
            return UserRole.ADMIN;
        }
        if (staffEmails.contains(normalized)) {
            return UserRole.STAFF;
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


