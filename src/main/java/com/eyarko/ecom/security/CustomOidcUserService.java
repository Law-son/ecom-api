package com.eyarko.ecom.security;

import com.eyarko.ecom.entity.User;
import com.eyarko.ecom.entity.UserRole;
import com.eyarko.ecom.repository.UserRepository;
import java.util.UUID;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom OIDC user service to load and persist user details from OIDC providers (e.g., Google with "openid" scope).
 * <p>
 * This service handles OIDC users (when using "openid" scope) and ensures a corresponding SQL {@link User} exists.
 * It works similarly to {@link CustomOAuth2UserService} but for OIDC flows.
 */
@Service
public class CustomOidcUserService extends OidcUserService {
    private static final Logger logger = LoggerFactory.getLogger(CustomOidcUserService.class);
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OAuth2RoleResolver roleResolver;
    private final CacheManager cacheManager;

    public CustomOidcUserService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        OAuth2RoleResolver roleResolver,
        CacheManager cacheManager
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleResolver = roleResolver;
        this.cacheManager = cacheManager;
    }

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String email = oidcUser.getEmail();
        String name = oidcUser.getFullName();

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(
                new org.springframework.security.oauth2.core.OAuth2Error("invalid_user"),
                "OIDC account has no email");
        }

        UserRole resolvedRole = roleResolver.resolveRole(email);

        User user = userRepository.findByEmailIgnoreCase(email)
            .map(existing -> {
                // CRITICAL: Reload user from database to ensure we have the latest role
                // This prevents any stale entity state issues
                User freshUser = userRepository.findById(existing.getId())
                    .orElse(existing);
                
                // Store original role from database BEFORE any modifications
                UserRole originalRole = freshUser.getRole();
                logger.debug("OIDC login - Original role from DB: {} for email: {}", originalRole, email);
                
                // Update full name from OIDC provider
                freshUser.setFullName(name != null && !name.isBlank() ? name : freshUser.getFullName());
                
                // Role preservation logic - CRITICAL: Never downgrade existing roles
                // Only upgrade if email is in allowlists, otherwise preserve original role
                if (shouldUpgradeRole(originalRole, resolvedRole)) {
                    // Only upgrade: CUSTOMER -> STAFF/ADMIN, or STAFF -> ADMIN
                    logger.info("OIDC role upgrade: {} -> {} for email: {}", originalRole, resolvedRole, email);
                    freshUser.setRole(resolvedRole);
                } else {
                    // CRITICAL: Preserve original role - never downgrade
                    // This ensures ADMIN/STAFF users never get downgraded to CUSTOMER
                    logger.info("OIDC role preserved: {} (resolved from allowlists: {}) for email: {}", 
                        originalRole, resolvedRole, email);
                    freshUser.setRole(originalRole); // Explicitly set to original role
                }
                
                return freshUser;
            })
            .orElseGet(() -> User.builder()
                .email(email)
                .fullName((name == null || name.isBlank()) ? email : name)
                // We keep password_hash non-null for DB constraints; OAuth2 users won't use it unless they set a password later.
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(resolvedRole)  // New users get role from allowlists
                .build());

        // Save user and flush to ensure role is persisted immediately
        // Store the role we want to preserve before saving
        UserRole roleToPreserve = user.getRole();
        logger.debug("Saving user with role: {} for email: {}", roleToPreserve, email);
        
        user = userRepository.save(user);
        userRepository.flush(); // Explicit flush to ensure role is persisted
        
        // Evict user cache to ensure fresh data is loaded next time
        if (user.getId() != null) {
            Cache userCache = cacheManager.getCache("userById");
            if (userCache != null) {
                userCache.evict(user.getId());
            }
        }
        
        // Defensive check: Verify role was preserved correctly
        // Reload from database to ensure we have the persisted value
        if (user.getId() != null) {
            User persistedUser = userRepository.findById(user.getId()).orElse(null);
            if (persistedUser != null) {
                UserRole persistedRole = persistedUser.getRole();
                if (!persistedRole.equals(roleToPreserve)) {
                    // Role was not preserved - this should never happen, but fix it
                    logger.warn("OIDC role mismatch detected! Expected: {}, Got: {} for user: {}. Restoring correct role.", 
                        roleToPreserve, persistedRole, email);
                    persistedUser.setRole(roleToPreserve);
                    persistedUser = userRepository.saveAndFlush(persistedUser);
                    // Evict cache again after fix
                    Cache userCache = cacheManager.getCache("userById");
                    if (userCache != null) {
                        userCache.evict(persistedUser.getId());
                    }
                    user = persistedUser;
                } else {
                    logger.debug("OIDC role verified: {} for email: {}", persistedRole, email);
                }
            }
        }

        // Return DefaultOidcUser with our user's authorities
        return new DefaultOidcUser(
            java.util.List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
            oidcUser.getIdToken(),
            oidcUser.getUserInfo()
        );
    }

    private static boolean shouldUpgradeRole(UserRole existingRole, UserRole resolvedRole) {
        if (resolvedRole == UserRole.CUSTOMER) {
            return false;
        }
        if (existingRole == UserRole.CUSTOMER && resolvedRole == UserRole.ADMIN) {
            return true;
        }
        return false;
    }
}

