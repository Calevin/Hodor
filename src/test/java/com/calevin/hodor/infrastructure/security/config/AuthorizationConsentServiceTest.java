package com.calevin.hodor.infrastructure.security.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = {
    "hodor.admin.username=test-consent-admin",
    "hodor.admin.password=test-pass",
    "hodor.auth.issuer-url=https://hodor-java-auth.calevin.com",
    "hodor.admin.client-id=test-consent-client",
    "hodor.admin.client-secret=test-secret"
})
@Testcontainers
class AuthorizationConsentServiceTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private OAuth2AuthorizationConsentService authorizationConsentService;

    @Autowired
    private RegisteredClientRepository registeredClientRepository;

    private final String clientId = "test-consent-client-" + UUID.randomUUID();
    private String internalId;

    @BeforeEach
    void setUp() {
        internalId = UUID.randomUUID().toString();
        // Asegurar que el cliente existe en el repositorio para que el servicio JDBC pueda validarlo
        RegisteredClient registeredClient = RegisteredClient.withId(internalId)
                .clientId(clientId)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost:8080/callback")
                .scope("openid")
                .scope("profile")
                .build();
        registeredClientRepository.save(registeredClient);
    }

    @Test
    @DisplayName("Debe persistir y recuperar el consentimiento de autorización correctamente")
    void testSaveAndFindConsent() {
        String principalName = "test-user-" + UUID.randomUUID();
        
        OAuth2AuthorizationConsent consent = OAuth2AuthorizationConsent.withId(internalId, principalName)
                .authority(new SimpleGrantedAuthority("SCOPE_openid"))
                .authority(new SimpleGrantedAuthority("SCOPE_profile"))
                .build();

        authorizationConsentService.save(consent);

        OAuth2AuthorizationConsent retrievedConsent = authorizationConsentService.findById(internalId, principalName);

        assertNotNull(retrievedConsent, "El consentimiento no debería ser nulo tras guardarlo");
        assertThat(retrievedConsent.getRegisteredClientId()).isEqualTo(internalId);
        assertThat(retrievedConsent.getPrincipalName()).isEqualTo(principalName);
        assertThat(retrievedConsent.getAuthorities()).hasSize(2);
        
        boolean hasOpenId = retrievedConsent.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("SCOPE_openid"));
        boolean hasProfile = retrievedConsent.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("SCOPE_profile"));
                
        assertThat(hasOpenId).isTrue();
        assertThat(hasProfile).isTrue();
    }
}
