package com.calevin.hodor.infrastructure.security.config;

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

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer() {
        return (context) -> {
            // Añadimos claims al Access Token
            if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType())) {
                String username = context.getPrincipal().getName();

                // 1. Recuperar los sistemas vinculados desde la DB
                var systems = new java.util.ArrayList<>(userRepository.findAuthorizedClientsByUsername(username));

                // 2. Inyectar el Custom Claim
                context.getClaims().claims(claims -> {
                    claims.put("authorized_systems", systems);

                    // Limpiar y estructurar mejor las authorities
                    var authorities = new java.util.ArrayList<>(context.getPrincipal().getAuthorities().stream()
                            .map(auth -> auth.getAuthority())
                            .toList());
                    claims.put("roles", authorities);
                });
            }
        };
    }
}