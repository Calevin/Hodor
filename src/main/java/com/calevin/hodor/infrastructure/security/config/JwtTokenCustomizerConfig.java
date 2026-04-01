package com.calevin.hodor.infrastructure.security.config;

import com.calevin.hodor.infrastructure.persistence.repositories.UserClientRoleRepository;
import com.calevin.hodor.infrastructure.persistence.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;

/**
 * Este componente intercepta la creación del token. Si el token que se está emitiendo es un access_token, inyectaremos el claim authorized_systems
 */
@Configuration
@RequiredArgsConstructor
public class JwtTokenCustomizerConfig {

    private final UserRepository userRepository;
    private final UserClientRoleRepository userClientRoleRepository;

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer() {
        return (context) -> {
            // Añadimos claims al Access Token
            if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
                String username = context.getPrincipal().getName();
                String clientId = context.getRegisteredClient().getClientId();

                // 1. Recuperar los sistemas vinculados desde la DB
                var systems = new java.util.ArrayList<>(userRepository.findAuthorizedClientsByUsername(username));

                // 2. Recuperar roles específicos para este cliente
                var clientSpecificRoles = userClientRoleRepository.findRolesByUsernameAndClientId(username, clientId);

                // 3. Obtener authorities globales (transversales)
                var globalRoles = context.getPrincipal().getAuthorities().stream()
                        .map(org.springframework.security.core.GrantedAuthority::getAuthority)
                        .toList();

                // 4. Consolidar roles sin duplicados y ordenados
                var allRoles = new java.util.TreeSet<>(globalRoles);
                allRoles.addAll(clientSpecificRoles);

                // Inyectar Custom Claims
                context.getClaims().claims(claims -> {
                    claims.put("authorized_systems", systems);
                    claims.put("roles", new java.util.ArrayList<>(allRoles));
                });
            }
        };
    }
}