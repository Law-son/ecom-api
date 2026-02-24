package com.eyarko.ecom.security;

import com.eyarko.ecom.entity.User;
import com.eyarko.ecom.entity.UserRole;
import com.eyarko.ecom.repository.UserRepository;
import java.util.Map;
import org.springframework.cache.CacheManager;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    
    private final UserRepository userRepository;
    private final OAuth2RoleResolver roleResolver;
    private final CacheManager cacheManager;

    public CustomOAuth2UserService(
        UserRepository userRepository,
        OAuth2RoleResolver roleResolver,
        CacheManager cacheManager
    ) {
        this.userRepository = userRepository;
        this.roleResolver = roleResolver;
        this.cacheManager = cacheManager;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        Map<String, Object> attrs = oauth2User.getAttributes();
        
        String email = String.valueOf(attrs.get("email"));
        String name = String.valueOf(attrs.get("name"));

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_user"), "No email from OAuth2 provider");
        }

        UserRole resolvedRole = roleResolver.resolveRole(email);

        User user = userRepository.findByEmailIgnoreCase(email)
            .map(existing -> updateExistingUser(existing, name, resolvedRole))
            .orElseGet(() -> createNewUser(email, name, resolvedRole));

        user = userRepository.saveAndFlush(user);
        evictUserCache(user.getId());

        return new UserPrincipalOAuth2User(UserPrincipal.fromUser(user), attrs);
    }

    private User updateExistingUser(User user, String name, UserRole resolvedRole) {
        if (name != null && !name.isBlank()) {
            user.setFullName(name);
        }
        if (shouldUpgradeRole(user.getRole(), resolvedRole)) {
            user.setRole(resolvedRole);
        }
        return user;
    }

    private User createNewUser(String email, String name, UserRole role) {
        return User.builder()
            .email(email)
            .fullName((name == null || name.isBlank()) ? email : name)
            .passwordHash(null)
            .role(role)
            .build();
    }

    private boolean shouldUpgradeRole(UserRole existing, UserRole resolved) {
        return existing == UserRole.CUSTOMER && resolved == UserRole.ADMIN;
    }

    private void evictUserCache(Long userId) {
        if (userId != null) {
            var cache = cacheManager.getCache("userById");
            if (cache != null) {
                cache.evict(userId);
            }
        }
    }
}
