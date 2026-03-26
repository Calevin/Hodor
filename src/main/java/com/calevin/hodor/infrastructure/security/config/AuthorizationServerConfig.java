package com.calevin.hodor.infrastructure.security.config;

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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

@Configuration
@EnableWebSecurity
public class AuthorizationServerConfig {

        @Bean
        @Order(1)
        public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
                // 1. Instanciamos el configurador
                OAuth2AuthorizationServerConfigurer authorizationServerConfigurer = new OAuth2AuthorizationServerConfigurer();

                http
                                .securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
                                .with(authorizationServerConfigurer, (authorizationServer) -> authorizationServer
                                                .oidc(Customizer.withDefaults()) // Habilita OpenID Connect 1.0
                                );

                http
                                // 2. Configuración de manejo de excepciones para el flujo de login
                                .exceptionHandling((exceptions) -> exceptions
                                                .defaultAuthenticationEntryPointFor(
                                                                new LoginUrlAuthenticationEntryPoint("/login"),
                                                                new MediaTypeRequestMatcher(MediaType.TEXT_HTML)))
                                // 3. Habilitamos el procesamiento de JWT para el UserInfo y Client Registration
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
                com.nimbusds.jose.jwk.RSAKey rsaKey = JwksUtils.generateRsa();
                com.nimbusds.jose.jwk.JWKSet jwkSet = new com.nimbusds.jose.jwk.JWKSet(rsaKey);
                return new com.nimbusds.jose.jwk.source.ImmutableJWKSet<>(jwkSet);
        }
}