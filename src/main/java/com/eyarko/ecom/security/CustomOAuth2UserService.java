package com.eyarko.ecom.security;

import com.eyarko.ecom.entity.User;
import com.eyarko.ecom.entity.UserRole;
import com.eyarko.ecom.repository.UserRepository;
import java.util.Map;
import java.util.UUID;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads a Google OAuth2 user and ensures a corresponding SQL {@link User} exists.
 * <p>
 * We persist the user so the rest of the app can use consistent authorization and auditing.
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2UserService.class);
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OAuth2RoleResolver roleResolver;
    private final CacheManager cacheManager;

    public CustomOAuth2UserService(
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
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        Map<String, Object> attrs = oauth2User.getAttributes();
        String email = asString(attrs.get("email"));
        String name = asString(attrs.get("name"));

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_user"), "Google account has no email");
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
                logger.debug("OAuth2 login - Original role from DB: {} for email: {}", originalRole, email);
                
                // Update full name from OAuth2 provider
                freshUser.setFullName(name != null && !name.isBlank() ? name : freshUser.getFullName());
                
                // Role preservation logic - CRITICAL: Never downgrade existing roles
                // Only upgrade if email is in allowlists, otherwise preserve original role
                if (shouldUpgradeRole(originalRole, resolvedRole)) {
                    // Only upgrade: CUSTOMER -> STAFF/ADMIN, or STAFF -> ADMIN
                    logger.info("OAuth2 role upgrade: {} -> {} for email: {}", originalRole, resolvedRole, email);
                    freshUser.setRole(resolvedRole);
                } else {
                    // CRITICAL: Preserve original role - never downgrade
                    // This ensures ADMIN/STAFF users never get downgraded to CUSTOMER
                    logger.info("OAuth2 role preserved: {} (resolved from allowlists: {}) for email: {}", 
                        originalRole, resolvedRole, email);
                    freshUser.setRole(originalRole); // Explicitly set to original role
                }
                
                return freshUser;
            })
            .orElseGet(() -> User.builder()
                .email(email)
                .fullName((name == null || name.isBlank()) ? email : name)
                .passwordHash(null)  // OAuth2 users have no password
                .role(resolvedRole)
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
                    logger.warn("OAuth2 role mismatch detected! Expected: {}, Got: {} for user: {}. Restoring correct role.", 
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
                    logger.debug("OAuth2 role verified: {} for email: {}", persistedRole, email);
                }
            }
        }

        // Preserve authorities via principal mapping used by the app
        return new UserPrincipalOAuth2User(UserPrincipal.fromUser(user), attrs);
    }

    private static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    /**
     * Determines if a role should be upgraded based on OAuth2 allowlists.
     * <p>
     * Role hierarchy: CUSTOMER < STAFF < ADMIN
     * <p>
     * Rules:
     * <ul>
     *   <li>Never downgrade (e.g., ADMIN -> CUSTOMER)</li>
     *   <li>Only upgrade if resolved role is higher than existing role</li>
     *   <li>If roles are equal, keep existing role</li>
     * </ul>
     *
     * @param existingRole The user's current role
     * @param resolvedRole The role resolved from OAuth2 allowlists
     * @return true if role should be upgraded, false otherwise
     */
    private static boolean shouldUpgradeRole(UserRole existingRole, UserRole resolvedRole) {
        // Never downgrade
        if (resolvedRole == UserRole.CUSTOMER) {
            return false; // Never downgrade to CUSTOMER
        }
        
        // Upgrade if resolved role is higher
        if (existingRole == UserRole.CUSTOMER && resolvedRole != UserRole.CUSTOMER) {
            return true; // Upgrade CUSTOMER to STAFF or ADMIN
        }
        if (existingRole == UserRole.STAFF && resolvedRole == UserRole.ADMIN) {
            return true; // Upgrade STAFF to ADMIN
        }
        
        // If roles are equal or existing is higher, don't change
        return false;
    }
}


