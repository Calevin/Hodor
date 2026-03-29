package com.calevin.hodor.infrastructure.security.config;

import java.time.Instant;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

import com.calevin.hodor.infrastructure.persistence.entities.KeyEntity;
import com.calevin.hodor.infrastructure.persistence.repositories.KeyRepository;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class AuthorizationServerConfig {

        @Value("${hodor.auth.issuer-url}")
        private String issuerUrl;

        private final KeyRepository keyRepository;

        @Bean
        @Order(1)
        public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
                OAuth2AuthorizationServerConfigurer authorizationServerConfigurer = new OAuth2AuthorizationServerConfigurer();

                http
                                // 1. Reclamamos los endpoints de OAuth2/OIDC
                                .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
                                .with(authorizationServerConfigurer, (authorizationServer) -> authorizationServer
                                                .oidc(Customizer.withDefaults()) // Habilita OpenID Connect 1.0
                                )
                                // 2. Permitir acceso público a los metadatos
                                .authorizeHttpRequests(authorize -> authorize
                                                .requestMatchers("/.well-known/jwks.json").permitAll()
                                                .requestMatchers("/.well-known/openid-configuration").permitAll()
                                                .anyRequest().authenticated())
                                // 3. Manejo de excepciones (Redirigir a login solo si es HTML y requiere auth)
                                .exceptionHandling((exceptions) -> exceptions
                                                .defaultAuthenticationEntryPointFor(
                                                                new LoginUrlAuthenticationEntryPoint("/login"),
                                                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)))
                                .oauth2ResourceServer((resourceServer) -> resourceServer
                                                .jwt(Customizer.withDefaults()));

                return http.build();
        }

        @Bean
        public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
                // Esta implementación buscará automáticamente en la tabla oauth2_registered_client
                return new JdbcRegisteredClientRepository(jdbcTemplate);
        }

        @Bean
        public com.nimbusds.jose.jwk.source.JWKSource<com.nimbusds.jose.proc.SecurityContext> jwkSource() {
                com.nimbusds.jose.jwk.RSAKey rsaKey = keyRepository.findFirstByOrderByCreatedAtDesc()
                                .map(entity -> {
                                        try {
                                                // Reconstruimos la llave desde el JSON guardado
                                                return com.nimbusds.jose.jwk.RSAKey.parse(entity.getKeyData());
                                        } catch (Exception e) {
                                                throw new RuntimeException("Error al parsear la llave persistida", e);
                                        }
                                })
                                .orElseGet(() -> {
                                        // Si no hay llave, generamos una nueva (solo pasará la primera vez)
                                        com.nimbusds.jose.jwk.RSAKey newKey = JwksUtils.generateRsa();
                                        KeyEntity entity = KeyEntity.builder()
                                                        .id(UUID.randomUUID())
                                                        .kid(newKey.getKeyID())
                                                        .keyData(newKey.toJSONString()) // Serializa pública y privada
                                                        .createdAt(Instant.now())
                                                        .build();
                                        keyRepository.save(entity);
                                        return newKey;
                                });

                com.nimbusds.jose.jwk.JWKSet jwkSet = new com.nimbusds.jose.jwk.JWKSet(rsaKey);
                return new com.nimbusds.jose.jwk.source.ImmutableJWKSet<>(jwkSet);
        }

        @Bean
        public AuthorizationServerSettings authorizationServerSettings() {
                // Define el issuer (quién emite el token). 
                // En tu laptop es localhost, en el servidor será tu dominio.
                return AuthorizationServerSettings.builder()
                                .issuer(issuerUrl)
                                .jwkSetEndpoint("/.well-known/jwks.json")
                                .build();
        }
}