package com.eyarko.ecom.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.eyarko.ecom.entity.User;
import com.eyarko.ecom.entity.UserRole;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class UserRepositoryTest {
    @Autowired
    private UserRepository userRepository;

    @Test
    void findByEmail_returnsUserWhenPresent() {
        User saved = userRepository.save(User.builder()
            .fullName("Jane Doe")
            .email("jane@example.com")
            .passwordHash("hashed")
            .role(UserRole.CUSTOMER)
            .build());

        Optional<User> found = userRepository.findByEmail("jane@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
    }
}

