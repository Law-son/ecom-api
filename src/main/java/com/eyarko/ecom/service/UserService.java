package com.eyarko.ecom.service;

import com.eyarko.ecom.dto.UserCreateRequest;
import com.eyarko.ecom.dto.UserResponse;
import com.eyarko.ecom.dto.UserUpdateRequest;
import com.eyarko.ecom.entity.User;
import com.eyarko.ecom.mapper.UserMapper;
import com.eyarko.ecom.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
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
    public UserResponse createUser(UserCreateRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        User user = User.builder()
            .fullName(request.getFullName())
            .email(request.getEmail())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .role(request.getRole())
            .build();
        return UserMapper.toResponse(userRepository.save(user));
    }

    /**
     * Retrieves a user by id.
     *
     * @param id user id
     * @return user details
     */
    public UserResponse getUser(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return UserMapper.toResponse(user);
    }

    /**
     * Lists all users.
     *
     * @return list of users
     */
    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream()
            .map(UserMapper::toResponse)
            .collect(Collectors.toList());
    }

    /**
     * Updates a user.
     *
     * @param id user id
     * @param request user payload
     * @return updated user
     */
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (request.getEmail() != null && !request.getEmail().equalsIgnoreCase(user.getEmail())) {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
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
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        userRepository.deleteById(id);
    }
}


