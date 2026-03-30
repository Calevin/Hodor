package com.calevin.hodor.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class TokenRevocationIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OAuth2AuthorizationService authorizationService;

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Debe revocar un access token correctamente")
    void testRevokeAccessToken() throws Exception {
        // 1. Crear y registrar un cliente para el test
        String clientId = "test-client-revocation-" + UUID.randomUUID();
        String clientSecret = "secret";
        RegisteredClient registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientSecret(passwordEncoder.encode(clientSecret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://localhost:8080/callback")
                .build();
        registeredClientRepository.save(registeredClient);

        // 2. Crear una autorización con un access token
        String tokenValue = "access-token-" + UUID.randomUUID();
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                tokenValue,
                Instant.now(),
                Instant.now().plus(Duration.ofHours(1))
        );

        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(registeredClient)
                .principalName("user")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .token(accessToken)
                .build();
        authorizationService.save(authorization);

        // 3. Verificar que el token existe antes de revocar
        assertThat(authorizationService.findByToken(tokenValue, OAuth2TokenType.ACCESS_TOKEN)).isNotNull();

        // 4. Llamar al endpoint de revocación
        mockMvc.perform(post("/oauth2/revoke")
                .with(httpBasic(clientId, clientSecret))
                .param("token", tokenValue)
                .param("token_type_hint", "access_token"))
                .andExpect(status().isOk());

        // 5. Verificar que el token haya sido invalidado
        OAuth2Authorization revokedAuth = authorizationService.findByToken(tokenValue, OAuth2TokenType.ACCESS_TOKEN);
        if (revokedAuth != null) {
            OAuth2Authorization.Token<OAuth2AccessToken> token = revokedAuth.getToken(tokenValue);
            org.junit.jupiter.api.Assertions.assertNotNull(token);
            assertThat(token.isInvalidated()).isTrue();
        }
    }

    @Test
    @DisplayName("Debe revocar un refresh token correctamente")
    void testRevokeRefreshToken() throws Exception {
        String clientId = "test-client-refresh-" + UUID.randomUUID();
        String clientSecret = "secret";
        RegisteredClient registeredClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientSecret(passwordEncoder.encode(clientSecret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://localhost:8080/callback")
                .build();
        registeredClientRepository.save(registeredClient);

        // Access token dummy para evitar NPEs internos que esperan una autorización completa
        String accessTokenValue = "access-token-dummy-" + UUID.randomUUID();
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                accessTokenValue,
                Instant.now(),
                Instant.now().plus(Duration.ofHours(1))
        );

        String tokenValue = "refresh-token-" + UUID.randomUUID();
        OAuth2RefreshToken refreshToken = new OAuth2RefreshToken(
                tokenValue,
                Instant.now(),
                Instant.now().plus(Duration.ofDays(1))
        );

        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(registeredClient)
                .principalName("user")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .token(accessToken)
                .refreshToken(refreshToken)
                .build();
        authorizationService.save(authorization);

        assertThat(authorizationService.findByToken(tokenValue, OAuth2TokenType.REFRESH_TOKEN)).isNotNull();

        mockMvc.perform(post("/oauth2/revoke")
                .with(httpBasic(clientId, clientSecret))
                .param("token", tokenValue)
                .param("token_type_hint", "refresh_token"))
                .andExpect(status().isOk());

        OAuth2Authorization revokedAuth = authorizationService.findByToken(tokenValue, OAuth2TokenType.REFRESH_TOKEN);
        if (revokedAuth != null) {
            OAuth2Authorization.Token<OAuth2RefreshToken> token = revokedAuth.getRefreshToken();
            org.junit.jupiter.api.Assertions.assertNotNull(token);
            assertThat(token.isInvalidated()).isTrue();
        }
    }
}
