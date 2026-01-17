package com.eyarko.ecom.service;

import com.eyarko.ecom.dto.UserCreateRequest;
import com.eyarko.ecom.dto.UserResponse;
import com.eyarko.ecom.entity.User;
import com.eyarko.ecom.mapper.UserMapper;
import com.eyarko.ecom.repository.UserRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse createUser(UserCreateRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        User user = User.builder()
            .fullName(request.getFullName())
            .email(request.getEmail())
            .passwordHash(request.getPassword())
            .role(request.getRole())
            .build();
        return UserMapper.toResponse(userRepository.save(user));
    }

    public UserResponse getUser(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        return UserMapper.toResponse(user);
    }

    public List<UserResponse> listUsers() {
        return userRepository.findAll().stream()
            .map(UserMapper::toResponse)
            .collect(Collectors.toList());
    }
}

