package com.calevin.hodor.application.services;

import com.calevin.hodor.application.dtos.ClientResponse;
import com.calevin.hodor.application.dtos.ClientUpdateRequest;
import com.calevin.hodor.application.exceptions.ClientNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientManagementServiceTest {

    @Mock
    private RegisteredClientRepository registeredClientRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private ClientManagementService clientManagementService;

    @BeforeEach
    void setUp() {
        clientManagementService = new ClientManagementService(registeredClientRepository, passwordEncoder);
    }

    private RegisteredClient buildSampleClient(String clientId) {
        return RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientSecret("hashed-secret-12345")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .redirectUri("https://old.domain.com/login/oauth2/code/hodor")
                .postLogoutRedirectUri("https://old.domain.com/logged-out")
                .scope("openid")
                .clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(true)
                        .requireProofKey(true)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(60))
                        .refreshTokenTimeToLive(Duration.ofDays(30))
                        .reuseRefreshTokens(false)
                        .build())
                .build();
    }

    @Test
    @DisplayName("updateClient_Success: Debe actualizar todos los campos mutables y persistir el cliente")
    void updateClient_Success() {
        String clientId = "service-client";
        RegisteredClient existing = buildSampleClient(clientId);
        when(registeredClientRepository.findByClientId(clientId)).thenReturn(existing);

        ClientUpdateRequest request = new ClientUpdateRequest(
                "https://new.domain.com/callback",
                "https://new.domain.com/post-logout",
                Set.of("openid", "profile"),
                120L,
                45L
        );

        ClientResponse response = clientManagementService.updateClient(clientId, request);

        ArgumentCaptor<RegisteredClient> captor = ArgumentCaptor.forClass(RegisteredClient.class);
        verify(registeredClientRepository).save(captor.capture());
        RegisteredClient saved = captor.getValue();

        assertThat(saved.getRedirectUris()).containsExactly("https://new.domain.com/callback");
        assertThat(saved.getPostLogoutRedirectUris()).containsExactly("https://new.domain.com/post-logout");
        assertThat(saved.getScopes()).containsExactlyInAnyOrder("openid", "profile");
        assertThat(saved.getTokenSettings().getAccessTokenTimeToLive()).isEqualTo(Duration.ofMinutes(120));
        assertThat(saved.getTokenSettings().getRefreshTokenTimeToLive()).isEqualTo(Duration.ofDays(45));

        assertThat(response.clientId()).isEqualTo(clientId);
        assertThat(response.redirectUri()).isEqualTo("https://new.domain.com/callback");
        assertThat(response.postLogoutRedirectUri()).isEqualTo("https://new.domain.com/post-logout");
        assertThat(response.scopes()).containsExactlyInAnyOrder("openid", "profile");
        assertThat(response.accessTokenTimeoutMinutes()).isEqualTo(120L);
        assertThat(response.refreshTokenTimeoutDays()).isEqualTo(45L);
    }

    @Test
    @DisplayName("updateClient_ClientIdIsImmutable: El clientId y el ID interno no deben mutar")
    void updateClient_ClientIdIsImmutable() {
        String clientId = "service-client-immutable";
        RegisteredClient existing = buildSampleClient(clientId);
        when(registeredClientRepository.findByClientId(clientId)).thenReturn(existing);

        ClientUpdateRequest request = new ClientUpdateRequest(
                "https://changed.domain.com/callback",
                null,
                null,
                null,
                null
        );

        clientManagementService.updateClient(clientId, request);

        ArgumentCaptor<RegisteredClient> captor = ArgumentCaptor.forClass(RegisteredClient.class);
        verify(registeredClientRepository).save(captor.capture());
        RegisteredClient saved = captor.getValue();

        assertThat(saved.getId()).isEqualTo(existing.getId());
        assertThat(saved.getClientId()).isEqualTo(clientId);
    }

    @Test
    @DisplayName("updateClient_PreservesSecretAndOmittedFromResponse: El secreto debe mantenerse intacto y no exponerse")
    void updateClient_PreservesSecretAndOmittedFromResponse() {
        String clientId = "service-client-secret";
        RegisteredClient existing = buildSampleClient(clientId);
        when(registeredClientRepository.findByClientId(clientId)).thenReturn(existing);

        ClientUpdateRequest request = new ClientUpdateRequest(
                "https://updated.domain.com/callback",
                null,
                null,
                null,
                null
        );

        ClientResponse response = clientManagementService.updateClient(clientId, request);

        ArgumentCaptor<RegisteredClient> captor = ArgumentCaptor.forClass(RegisteredClient.class);
        verify(registeredClientRepository).save(captor.capture());
        RegisteredClient saved = captor.getValue();

        assertThat(saved.getClientSecret()).isEqualTo("hashed-secret-12345");
        // ClientResponse no expone ningun secreto
        assertThat(response.clientId()).isEqualTo(clientId);
    }

    @Test
    @DisplayName("updateClient_RemovesPostLogoutRedirectUriWhenEmpty: Cadena vacia remueve la postLogoutRedirectUri")
    void updateClient_RemovesPostLogoutRedirectUriWhenEmpty() {
        String clientId = "service-client-logout-clean";
        RegisteredClient existing = buildSampleClient(clientId);
        when(registeredClientRepository.findByClientId(clientId)).thenReturn(existing);

        ClientUpdateRequest request = new ClientUpdateRequest(
                null,
                "",
                null,
                null,
                null
        );

        ClientResponse response = clientManagementService.updateClient(clientId, request);

        ArgumentCaptor<RegisteredClient> captor = ArgumentCaptor.forClass(RegisteredClient.class);
        verify(registeredClientRepository).save(captor.capture());
        RegisteredClient saved = captor.getValue();

        assertThat(saved.getPostLogoutRedirectUris()).isEmpty();
        assertThat(response.postLogoutRedirectUri()).isNull();
    }

    @Test
    @DisplayName("updateClient_PreservesOmittedFields: Campos nulos en la peticion deben preservar sus valores originales")
    void updateClient_PreservesOmittedFields() {
        String clientId = "service-client-partial";
        RegisteredClient existing = buildSampleClient(clientId);
        when(registeredClientRepository.findByClientId(clientId)).thenReturn(existing);

        ClientUpdateRequest request = new ClientUpdateRequest(
                null,
                "https://brand-new.domain.com/logout",
                null,
                null,
                null
        );

        ClientResponse response = clientManagementService.updateClient(clientId, request);

        ArgumentCaptor<RegisteredClient> captor = ArgumentCaptor.forClass(RegisteredClient.class);
        verify(registeredClientRepository).save(captor.capture());
        RegisteredClient saved = captor.getValue();

        assertThat(saved.getRedirectUris()).containsExactly("https://old.domain.com/login/oauth2/code/hodor");
        assertThat(saved.getPostLogoutRedirectUris()).containsExactly("https://brand-new.domain.com/logout");
        assertThat(saved.getScopes()).containsExactly("openid");
        assertThat(saved.getTokenSettings().getAccessTokenTimeToLive()).isEqualTo(Duration.ofMinutes(60));
        assertThat(saved.getTokenSettings().getRefreshTokenTimeToLive()).isEqualTo(Duration.ofDays(30));

        assertThat(response.redirectUri()).isEqualTo("https://old.domain.com/login/oauth2/code/hodor");
        assertThat(response.postLogoutRedirectUri()).isEqualTo("https://brand-new.domain.com/logout");
    }

    @Test
    @DisplayName("updateClient_ThrowsWhenClientNotFound: Debe lanzar ClientNotFoundException si el cliente no existe")
    void updateClient_ThrowsWhenClientNotFound() {
        String clientId = "non-existent-client";
        when(registeredClientRepository.findByClientId(clientId)).thenReturn(null);

        ClientUpdateRequest request = new ClientUpdateRequest(
                "https://domain.com/callback",
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> clientManagementService.updateClient(clientId, request))
                .isInstanceOf(ClientNotFoundException.class)
                .hasMessageContaining("non-existent-client");
    }
}
