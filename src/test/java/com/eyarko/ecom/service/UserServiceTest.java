package com.eyarko.ecom.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eyarko.ecom.dto.UserUpdateRequest;
import com.eyarko.ecom.entity.User;
import com.eyarko.ecom.entity.UserRole;
import com.eyarko.ecom.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void updateUser_updatesFieldsWhenProvided() {
        User existing = User.builder()
            .id(1L)
            .fullName("Old Name")
            .email("old@example.com")
            .passwordHash("old")
            .role(UserRole.CUSTOMER)
            .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserUpdateRequest request = UserUpdateRequest.builder()
            .fullName("New Name")
            .email("new@example.com")
            .role(UserRole.ADMIN)
            .build();

        userService.updateUser(1L, request);

        verify(userRepository).save(any(User.class));
        assertThat(existing.getFullName()).isEqualTo("New Name");
        assertThat(existing.getEmail()).isEqualTo("new@example.com");
        assertThat(existing.getRole()).isEqualTo(UserRole.ADMIN);
    }
}

