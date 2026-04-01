package com.calevin.hodor.application.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import com.calevin.hodor.application.dtos.UserResponse;
import com.calevin.hodor.infrastructure.persistence.entities.UserEntity;
import com.calevin.hodor.infrastructure.persistence.repositories.UserClientRoleRepository;
import com.calevin.hodor.infrastructure.persistence.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

        @Mock
        private UserRepository userRepository;

        @Mock
        private RegisteredClientRepository clientRepository;

        @Mock
        private UserClientRoleRepository userClientRoleRepository;

        @Mock
        private PasswordEncoder passwordEncoder;

        private UserAdminService userAdminService;

        @BeforeEach
        void setUp() {
                userAdminService = new UserAdminService(userRepository, userClientRoleRepository, clientRepository,
                                passwordEncoder);
        }

        @Test
        @DisplayName("findAllUsers debe retornar una lista de UserResponse con autoridades y sistemas correctamente mapeados")
        void testFindAllUsers() {
                // Given
                UUID userId = UUID.randomUUID();
                UserEntity user = UserEntity.builder()
                                .id(userId)
                                .username("test-user")
                                .enabled(true)
                                .build();
                user.addAuthority("ROLE_USER");
                user.addAuthority("ROLE_ADMIN");

                when(userRepository.findAll()).thenReturn(List.of(user));
                when(userRepository.findAuthorizedClientsByUsername("test-user"))
                                .thenReturn(List.of("client-1", "client-2"));
                when(userClientRoleRepository.findRolesByUsernameAndClientId("test-user", "client-1"))
                                .thenReturn(List.of("ROLE_EDITOR"));
                when(userClientRoleRepository.findRolesByUsernameAndClientId("test-user", "client-2"))
                                .thenReturn(List.of("ROLE_PLAYER"));

                // When
                List<UserResponse> result = userAdminService.findAllUsers();

                // Then
                assertThat(result).hasSize(1);
                UserResponse response = result.get(0);
                assertThat(response.id()).isEqualTo(userId);
                assertThat(response.username()).isEqualTo("test-user");
                assertThat(response.enabled()).isTrue();
                assertThat(response.authorities()).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
                assertThat(response.authorizedSystems())
                                .extracting("clientId")
                                .containsExactlyInAnyOrder("client-1", "client-2");

                assertThat(response.authorizedSystems())
                                .filteredOn(s -> s.clientId().equals("client-1"))
                                .flatExtracting("roles")
                                .containsExactly("ROLE_EDITOR");
        }

        @Test
        @DisplayName("assignRoleToUserForClient debe lanzar excepción si el usuario no existe")
        void testAssignRoleToUserForClient_UserNotFound() {
                UUID userId = UUID.randomUUID();
                when(userRepository.findById(userId)).thenReturn(java.util.Optional.empty());

                org.assertj.core.api.Assertions
                                .assertThatThrownBy(() -> userAdminService.assignRoleToUserForClient(userId,
                                                new com.calevin.hodor.application.dtos.RoleAssignmentRequest("client",
                                                                "ROLE_TEST")))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("Usuario con ID");
        }
}
