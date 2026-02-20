package com.eyarko.ecom.security;

import com.eyarko.ecom.entity.User;
import com.eyarko.ecom.entity.UserRole;
import com.eyarko.ecom.repository.UserRepository;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads a Google OAuth2 user and ensures a corresponding SQL {@link User} exists.
 * <p>
 * We persist the user so the rest of the app can use consistent authorization and auditing.
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OAuth2RoleResolver roleResolver;

    public CustomOAuth2UserService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        OAuth2RoleResolver roleResolver
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleResolver = roleResolver;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        Map<String, Object> attrs = oauth2User.getAttributes();
        String email = asString(attrs.get("email"));
        String name = asString(attrs.get("name"));

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_user"), "Google account has no email");
        }

        UserRole role = roleResolver.resolveRole(email);

        User user = userRepository.findByEmailIgnoreCase(email)
            .map(existing -> {
                existing.setFullName(name != null && !name.isBlank() ? name : existing.getFullName());
                existing.setRole(role);
                return existing;
            })
            .orElseGet(() -> User.builder()
                .email(email)
                .fullName((name == null || name.isBlank()) ? email : name)
                // We keep password_hash non-null for DB constraints; OAuth2 users won't use it unless they set a password later.
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(role)
                .build());

        userRepository.save(user);

        // Preserve authorities via principal mapping used by the app
        return new UserPrincipalOAuth2User(UserPrincipal.fromUser(user), attrs);
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}


