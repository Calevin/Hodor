package com.calevin.hodor.application.services;

import com.calevin.hodor.application.dtos.ClientRegistrationRequest;
import com.calevin.hodor.application.dtos.ClientResponse;
import com.calevin.hodor.application.dtos.ClientUpdateRequest;
import com.calevin.hodor.application.exceptions.ClientNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Servicio de aplicacion encargado de la gestion y ciclo de vida de RegisteredClient.
 */
@Service
@RequiredArgsConstructor
public class ClientManagementService {

    private final RegisteredClientRepository registeredClientRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
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
                .redirectUri(request.redirectUri());

        if (request.postLogoutRedirectUri() != null) {
            builder.postLogoutRedirectUri(request.postLogoutRedirectUri());
        }

        builder.clientSettings(ClientSettings.builder()
                        .requireAuthorizationConsent(true)
                        .requireProofKey(true)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofMinutes(accessMinutes))
                        .refreshTokenTimeToLive(Duration.ofDays(refreshDays))
                        .reuseRefreshTokens(false) // Rotacion obligatoria por seguridad
                        .build());

        // Agregamos los scopes dinamicamente
        request.scopes().forEach(builder::scope);

        registeredClientRepository.save(builder.build());
    }

    /**
     * Actualiza de forma parcial los atributos mutables de un cliente registrado.
     * Preserva intactos el identificador, el secreto y los campos no enviados.
     */
    @Transactional
    public ClientResponse updateClient(String clientId, ClientUpdateRequest request) {
        RegisteredClient existing = registeredClientRepository.findByClientId(clientId);
        if (existing == null) {
            throw new ClientNotFoundException("El cliente '" + clientId + "' no existe.");
        }

        RegisteredClient.Builder builder = RegisteredClient.from(existing);

        // Actualizacion selectiva de redirectUri si fue provista
        if (request.redirectUri() != null) {
            builder.redirectUris(uris -> {
                uris.clear();
                uris.add(request.redirectUri());
            });
        }

        // Actualizacion selectiva de postLogoutRedirectUri (cadena vacia para remocion)
        if (request.postLogoutRedirectUri() != null) {
            if (request.postLogoutRedirectUri().isBlank()) {
                builder.postLogoutRedirectUris(Set::clear);
            } else {
                builder.postLogoutRedirectUris(uris -> {
                    uris.clear();
                    uris.add(request.postLogoutRedirectUri());
                });
            }
        }

        // Actualizacion selectiva de scopes si el conjunto viene provisto y no vacio
        if (request.scopes() != null && !request.scopes().isEmpty()) {
            builder.scopes(scopes -> {
                scopes.clear();
                scopes.addAll(request.scopes());
            });
        }

        // Actualizacion selectiva de configuracion de tokens si se indican nuevos tiempos
        if (request.accessTokenTimeoutMinutes() != null || request.refreshTokenTimeoutDays() != null) {
            TokenSettings.Builder tokenSettingsBuilder = TokenSettings.withSettings(existing.getTokenSettings().getSettings());
            if (request.accessTokenTimeoutMinutes() != null) {
                tokenSettingsBuilder.accessTokenTimeToLive(Duration.ofMinutes(request.accessTokenTimeoutMinutes()));
            }
            if (request.refreshTokenTimeoutDays() != null) {
                tokenSettingsBuilder.refreshTokenTimeToLive(Duration.ofDays(request.refreshTokenTimeoutDays()));
            }
            builder.tokenSettings(tokenSettingsBuilder.build());
        }

        RegisteredClient updated = builder.build();
        registeredClientRepository.save(updated);

        String currentRedirectUri = updated.getRedirectUris().stream().findFirst().orElse(null);
        String currentPostLogoutRedirectUri = updated.getPostLogoutRedirectUris().stream().findFirst().orElse(null);
        Long currentAccessMinutes = updated.getTokenSettings().getAccessTokenTimeToLive() != null
                ? updated.getTokenSettings().getAccessTokenTimeToLive().toMinutes()
                : null;
        Long currentRefreshDays = updated.getTokenSettings().getRefreshTokenTimeToLive() != null
                ? updated.getTokenSettings().getRefreshTokenTimeToLive().toDays()
                : null;

        return new ClientResponse(
                updated.getClientId(),
                currentRedirectUri,
                currentPostLogoutRedirectUri,
                updated.getScopes(),
                currentAccessMinutes,
                currentRefreshDays
        );
    }
}
