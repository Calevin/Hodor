package com.calevin.hodor.application.services;

import com.calevin.hodor.application.dtos.ClientRegistrationRequest;
import com.calevin.hodor.application.dtos.UserRegistrationRequest;
import com.calevin.hodor.infrastructure.persistence.entities.UserEntity;
import com.calevin.hodor.infrastructure.persistence.repositories.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationServicesTest {

        @Nested
        @DisplayName("Tests para ClientManagementService")
        class ClientManagementServiceTests {
                @Mock
                private RegisteredClientRepository registeredClientRepository;
                @Mock
                private PasswordEncoder passwordEncoder;
                @InjectMocks
                private ClientManagementService clientManagementService;

                @Test
                @DisplayName("Debe registrar un cliente correctamente")
                void registerClient_Success() {
                        String clientId = "test-client";
                        String secret = "secret";
                        String encodedSecret = "encoded-secret";
                        String redirectUri = "http://localhost/callback";
                        Set<String> scopes = Set.of("openid", "api:read", "api:write");
                        Long accessTokenTimeoutMinutes = 15L;
                        Long refreshTokenTimeoutDays = 7L;

                        when(passwordEncoder.encode(secret)).thenReturn(encodedSecret);

                        ClientRegistrationRequest request = new ClientRegistrationRequest(clientId, secret, redirectUri,
                                        "http://localhost/logout-callback",
                                        scopes,
                                        accessTokenTimeoutMinutes, refreshTokenTimeoutDays);
                        clientManagementService.registerClient(request);

                        ArgumentCaptor<RegisteredClient> captor = ArgumentCaptor.forClass(RegisteredClient.class);
                        verify(registeredClientRepository).save(captor.capture());
                        RegisteredClient savedClient = captor.getValue();

                        assertThat(savedClient.getClientId()).isEqualTo(clientId);
                        assertThat(savedClient.getClientSecret()).isEqualTo(encodedSecret);
                        assertThat(savedClient.getRedirectUris()).contains(redirectUri);
                        assertThat(savedClient.getPostLogoutRedirectUris()).contains("http://localhost/logout-callback");
                        assertThat(savedClient.getScopes()).containsAll(scopes);

                        // Validar que los timeouts se guardaron correctamente
                        assertThat(savedClient.getTokenSettings().getAccessTokenTimeToLive())
                                        .isEqualTo(java.time.Duration.ofMinutes(accessTokenTimeoutMinutes));
                        assertThat(savedClient.getTokenSettings().getRefreshTokenTimeToLive())
                                        .isEqualTo(java.time.Duration.ofDays(refreshTokenTimeoutDays));
                }

                @Test
                @DisplayName("Debe registrar un cliente con valores por defecto si no se indican timeouts")
                void registerClient_WithDefaultTimeouts_Success() {
                        String clientId = "test-client-default";
                        String secret = "secret";
                        String redirectUri = "http://localhost/callback";
                        Set<String> scopes = Set.of("openid");

                        // Request con timeouts nulos
                        ClientRegistrationRequest request = new ClientRegistrationRequest(clientId, secret, redirectUri,
                                        null, scopes, null, null);

                        clientManagementService.registerClient(request);

                        ArgumentCaptor<RegisteredClient> captor = ArgumentCaptor.forClass(RegisteredClient.class);
                        verify(registeredClientRepository).save(captor.capture());
                        RegisteredClient savedClient = captor.getValue();

                        // Validar valores por defecto: 60 min para access, 30 días para refresh
                        assertThat(savedClient.getTokenSettings().getAccessTokenTimeToLive())
                                        .isEqualTo(java.time.Duration.ofMinutes(60));
                        assertThat(savedClient.getTokenSettings().getRefreshTokenTimeToLive())
                                        .isEqualTo(java.time.Duration.ofDays(30));
                }
        }

        @Nested
        @DisplayName("Tests para UserAdminService")
        class UserAdminServiceTests {
                @Mock
                private UserRepository userRepository;
                @Mock
                private RegisteredClientRepository clientRepository;
                @Mock
                private PasswordEncoder passwordEncoder;
                @InjectMocks
                private UserAdminService userAdminService;

                @Test
                @DisplayName("Debe registrar un usuario correctamente")
                void registerUser_Success() {
                        UserRegistrationRequest request = new UserRegistrationRequest("user", "pass", "client");
                        RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
                                        .clientId("client")
                                        .authorizationGrantType(
                                                        org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
                                        .redirectUri("http://localhost/callback")
                                        .build();

                        when(clientRepository.findByClientId("client")).thenReturn(client);
                        when(userRepository.findByUsername("user")).thenReturn(Optional.empty());
                        when(passwordEncoder.encode("pass")).thenReturn("encoded-pass");

                        // UserEntity has auto-generated values, we'll manually set ID for the link test
                        // Simulate JPA ID assignment
                        doAnswer(invocation -> invocation.getArgument(0))
                                        .when(userRepository).save(any(UserEntity.class));

                        userAdminService.registerUserForSystem(request);

                        verify(userRepository).save(any(UserEntity.class));
                        verify(userRepository).linkUserToClient(any(), eq(client.getId()));
                }

                @Test
                @DisplayName("Debe fallar si el cliente no existe")
                void registerUser_ClientNotFound() {
                        UserRegistrationRequest request = new UserRegistrationRequest("user", "pass", "unknown");
                        when(clientRepository.findByClientId("unknown")).thenReturn(null);

                        assertThatThrownBy(() -> userAdminService.registerUserForSystem(request))
                                        .isInstanceOf(IllegalArgumentException.class)
                                        .hasMessageContaining("no está registrado");
                }

                @Test
                @DisplayName("Debe fallar si el usuario ya existe")
                void registerUser_UserAlreadyExists() {
                        UserRegistrationRequest request = new UserRegistrationRequest("user", "pass", "client");
                        RegisteredClient client = RegisteredClient.withId("id")
                                        .clientId("client")
                                        .authorizationGrantType(
                                                        org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
                                        .redirectUri("http://localhost/callback")
                                        .build();

                        when(clientRepository.findByClientId("client")).thenReturn(client);
                        when(userRepository.findByUsername("user")).thenReturn(Optional.of(new UserEntity()));

                        assertThatThrownBy(() -> userAdminService.registerUserForSystem(request))
                                        .isInstanceOf(IllegalStateException.class)
                                        .hasMessageContaining("ya existe");
                }
        }
}
