package com.calevin.hodor.infrastructure.bootstrap;

import com.calevin.hodor.infrastructure.persistence.entities.UserEntity;
import com.calevin.hodor.infrastructure.persistence.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder {

    private final UserRepository userRepository;
    private final RegisteredClientRepository registeredClientRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${hodor.admin.username}")
    private String adminUsername;

    @Value("${hodor.admin.password}")
    private String adminPassword;

    @Value("${hodor.admin.client-id}")
    private String adminClientId;

    @Value("${hodor.admin.client-secret}")
    private String adminClientSecret;

    @Value("${hodor.auth.issuer-url}")
    private String issuerUrl;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedAdminUser() {
        log.info("Iniciando proceso de bootstrap de Hodor...");

        validateEnvironmentVariables();

        // 1. Verificación de existencia del Cliente de Gestión
        if (registeredClientRepository.findByClientId(adminClientId) != null) {
            log.info("Cliente de gestion '{}' ya presente. Omitiendo inicializacion.", adminClientId);
            return;
        }

        // 2. Crear el Cliente de Gestión (hodor-admin-cli)
        RegisteredClient adminClient = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(adminClientId)
                .clientSecret(passwordEncoder.encode(adminClientSecret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri(issuerUrl + "/callback") // URL de redirección administrativa
                .scope(OidcScopes.OPENID)
                .scope("admin:read")
                .scope("admin:write")
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(false)
                        .requireAuthorizationConsent(true)
                        .build())
                .build();

        registeredClientRepository.save(adminClient);
        log.info("Hodor: Cliente de gestion '{}' creado.", adminClientId);

        // 3. Crear el Usuario Administrador (si no existe)
        UserEntity adminUser = userRepository.findByUsername(adminUsername)
                .orElseGet(() -> {
                    var newUser = UserEntity.builder()
                            .username(adminUsername)
                            .password(passwordEncoder.encode(adminPassword))
                            .enabled(true)
                            .build();
                    newUser.addAuthority("ROLE_ADMIN");
                    return userRepository.save(newUser);
                });

        // 4. Vincular Usuario con Cliente en la tabla intermedia
        // Usamos el método nativo que implementamos en el repositorio
        userRepository.linkUserToClient(adminUser.getId(), adminClient.getId());

        log.info("Hodor: Vinculo creado entre '{}' y '{}'.", adminUsername, adminClientId);
    }

    private void validateEnvironmentVariables() {
        if (adminUsername.isBlank() || adminPassword.isBlank() || adminClientId.isBlank() || adminClientSecret.isBlank()
                || issuerUrl.isBlank()) {
            log.error(
                    "ERROR CRÍTICO: Las variables AUTH_ADMIN_USER, AUTH_ADMIN_PASS, AUTH_ADMIN_CLIENT_ID, AUTH_ADMIN_CLIENT_SECRET y AUTH_ISSUER_URL son obligatorias para el primer inicio.");
            log.error("El sistema no puede garantizar el acceso administrativo. Abortando...");
            System.exit(1); // Fail-fast mechanism
        }
    }
}