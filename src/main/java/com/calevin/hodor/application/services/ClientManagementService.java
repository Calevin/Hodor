package com.calevin.hodor.application.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;

import com.calevin.hodor.application.dtos.ClientRegistrationRequest;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * Este servicio encapsula la lógica para crear un RegisteredClient
 */
@Service
@RequiredArgsConstructor
public class ClientManagementService {

        private final RegisteredClientRepository registeredClientRepository;
        private final PasswordEncoder passwordEncoder;

        public void registerClient(ClientRegistrationRequest request) {
                if (registeredClientRepository.findByClientId(request.clientId()) != null) {
                        throw new IllegalStateException("El cliente '" + request.clientId() + "' ya existe.");
                }

                // Definimos valores por defecto si vienen nulos
                long accessMinutes = Objects.requireNonNullElse(request.accessTokenTimeoutMinutes(), 60L);
                long refreshDays = Objects.requireNonNullElse(request.refreshTokenTimeoutDays(), 30L);

                RegisteredClient.Builder builder = RegisteredClient.withId(UUID.randomUUID().toString())
                                .clientId(request.clientId())
                                .clientSecret(passwordEncoder.encode(request.clientSecret()))
                                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                                .redirectUri(request.redirectUri())
                                .clientSettings(ClientSettings.builder()
                                                .requireAuthorizationConsent(true)
                                                .requireProofKey(true)
                                                .build())
                                .tokenSettings(TokenSettings.builder()
                                                .accessTokenTimeToLive(Duration.ofMinutes(accessMinutes))
                                                .refreshTokenTimeToLive(Duration.ofDays(refreshDays))
                                                .reuseRefreshTokens(false) // Rotación obligatoria por seguridad
                                                .build());

                // Agregamos los scopes dinámicamente
                request.scopes().forEach(builder::scope);

                registeredClientRepository.save(builder.build());
        }
}