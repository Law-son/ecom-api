package com.eyarko.ecom.service;

import com.eyarko.ecom.dto.PagedResponse;
import com.eyarko.ecom.dto.UserCreateRequest;
import com.eyarko.ecom.dto.UserResponse;
import com.eyarko.ecom.dto.UserUpdateRequest;
import com.eyarko.ecom.entity.User;
import com.eyarko.ecom.entity.UserRole;
import com.eyarko.ecom.mapper.UserMapper;
import com.eyarko.ecom.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * User management business logic.
 */
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Creates a user.
     *
     * @param request user payload
     * @return created user
     */
    @CacheEvict(value = "users", allEntries = true)
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        if (userRepository.findByEmailIgnoreCase(request.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        UserRole resolvedRole = resolveRole(request);
        User user = User.builder()
            .fullName(request.getFullName())
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .role(resolvedRole)
            .build();
        return UserMapper.toResponse(userRepository.save(user));
    }

    /**
     * Retrieves a user by id.
     *
     * @param id user id
     * @return user details
     */
    @Cacheable(value = "users", key = "'user:' + #id")
    @Transactional(readOnly = true)
    public UserResponse getUser(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return UserMapper.toResponse(user);
    }

    /**
     * Lists users with pagination.
     *
     * @param pageable paging and sorting options
     * @return paged list of users
     */
    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> listUsers(Pageable pageable) {
        Page<User> page = userRepository.findAllUsers(pageable);
        List<UserResponse> items = page.getContent().stream()
            .map(UserMapper::toResponse)
            .collect(Collectors.toList());
        return PagedResponse.<UserResponse>builder()
            .items(items)
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .hasNext(page.hasNext())
            .hasPrevious(page.hasPrevious())
            .build();
    }

    /**
     * Updates a user.
     *
     * @param id user id
     * @param request user payload
     * @return updated user
     */
    @CacheEvict(value = "users", allEntries = true)
    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (request.getEmail() != null && !request.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.findByEmailIgnoreCase(request.getEmail()).isPresent()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPassword() != null) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        return UserMapper.toResponse(userRepository.save(user));
    }

    /**
     * Deletes a user by id.
     *
     * @param id user id
     */
    @CacheEvict(value = "users", allEntries = true)
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        userRepository.deleteById(id);
    }

    private UserRole resolveRole(UserCreateRequest request) {
        if (request.getRole() == null) {
            return UserRole.CUSTOMER;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication != null
            && authentication.isAuthenticated()
            && authentication.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        return isAdmin ? request.getRole() : UserRole.CUSTOMER;
    }
}


