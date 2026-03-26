package com.calevin.hodor.infrastructure.persistence.repositories;

import com.calevin.hodor.infrastructure.persistence.entities.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class UserRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private UserRepository userRepository;

    @Test
    void testFindByUsername_ReturnsUser_WhenUserExists() {
        // Arrange
        UserEntity user = UserEntity.builder()
                .username("jon.snow")
                .password("hashed_password")
                .email("jon.snow@winterfell.com")
                .enabled(true)
                .build();
        userRepository.save(user);

        // Act
        Optional<UserEntity> result = userRepository.findByUsername("jon.snow");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("jon.snow");
        assertThat(result.get().getEmail()).isEqualTo("jon.snow@winterfell.com");
    }

    @Test
    void testFindByUsername_ReturnsEmpty_WhenUserDoesNotExist() {
        // Act
        Optional<UserEntity> result = userRepository.findByUsername("unknown_user");

        // Assert
        assertThat(result).isNotPresent();
    }

    @Test
    void testExistsByAuthorities_Authority_ReturnsTrue_WhenRoleMatches() {
        // Arrange
        UserEntity user = UserEntity.builder()
                .username("arya.stark")
                .password("hashed_password")
                .email("arya.stark@winterfell.com")
                .enabled(true)
                .build();
        
        user.addAuthority("ROLE_ADMIN");
        userRepository.save(user);

        // Act
        boolean existsAdmin = userRepository.existsByAuthorities_Authority("ROLE_ADMIN");

        // Assert
        assertThat(existsAdmin).isTrue();
    }

    @Test
    void testExistsByAuthorities_Authority_ReturnsFalse_WhenRoleDoesNotMatch() {
        // Arrange
        UserEntity user = UserEntity.builder()
                .username("sansa.stark")
                .password("hashed_password")
                .email("sansa.stark@winterfell.com")
                .enabled(true)
                .build();
        
        user.addAuthority("ROLE_USER");
        userRepository.save(user);

        // Act
        boolean existsAdmin = userRepository.existsByAuthorities_Authority("ROLE_ADMIN");

        // Assert
        assertThat(existsAdmin).isFalse();
    }
}
