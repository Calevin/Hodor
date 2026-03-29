package com.calevin.hodor.infrastructure.security.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "hodor.admin.username=test-admin",
        "hodor.admin.password=test-pass",
        "hodor.auth.issuer-url=https://hodor-java-auth.calevin.com",
        "hodor.admin.client-id=test-client-id",
        "hodor.admin.client-secret=test-client-secret"
})
@AutoConfigureMockMvc
@Testcontainers
class AuthorizationServerConfigTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.calevin.hodor.infrastructure.persistence.repositories.KeyRepository keyRepository;

    @Test
    @DisplayName("El endpoint de JWKS debe devolver una clave RSA generada por JwksUtils")
    void testJwksEndpointReturnsKeys() throws Exception {
        mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys").isArray())
                .andExpect(jsonPath("$.keys").isNotEmpty())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"))
                .andExpect(jsonPath("$.keys[0].e").exists())
                .andExpect(jsonPath("$.keys[0].n").exists())
                .andExpect(jsonPath("$.keys[0].kid").exists());
    }

    @Test
    @DisplayName("La llave JWKS debe persistirse en la base de datos y ser unica")
    void testJwksKeyIsPersistedAndUnique() throws Exception {
        // Al arrancar el test, el bean jwkSource ya debió ejecutarse y crear una llave
        long initialCount = keyRepository.count();
        var keys = keyRepository.findAll();
        
        assertThat(initialCount).isGreaterThanOrEqualTo(1L);
        String kidInDb = keys.get(0).getKid();

        // Llamamos al endpoint para asegurar que no se generan llaves nuevas por cada peticion
        mockMvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].kid").value(kidInDb));

        assertThat(keyRepository.count()).isEqualTo(initialCount);
    }

    @Test
    @DisplayName("El endpoint de configuracion OIDC debe usar el emisor configurado")
    void testOidcDiscoveryEndpointUsesConfiguredIssuer() throws Exception {
        String expectedIssuer = "https://hodor-java-auth.calevin.com";

        mockMvc.perform(get("/.well-known/openid-configuration"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").value(expectedIssuer))
                .andExpect(jsonPath("$.authorization_endpoint").value(containsString(expectedIssuer)))
                .andExpect(jsonPath("$.token_endpoint").value(containsString(expectedIssuer)))
                .andExpect(jsonPath("$.jwks_uri").value(containsString(expectedIssuer)));
    }
}
