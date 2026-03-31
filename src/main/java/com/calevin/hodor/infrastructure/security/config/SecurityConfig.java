package com.calevin.hodor.infrastructure.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @Order(2) // Prioridad baja para dejar que el Authorization Server tome las rutas de OAuth2
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http, 
            org.springframework.security.core.session.SessionRegistry sessionRegistry) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/.well-known/jwks.json").permitAll() // Público para validación externa
                        .requestMatchers("/api/admin/**").hasRole("ADMIN") // Protegido para el Seeder/Admin
                        .requestMatchers("/login", "/css/**", "/js/**", "/images/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .formLogin(login -> login
                        .loginPage("/login")
                        .failureHandler((request, response, exception) -> {
                            String username = request.getParameter("username");
                            request.getSession().setAttribute("SPRING_SECURITY_LAST_USERNAME", username);
                            response.sendRedirect("/login?error");
                        })
                        .permitAll()) // Habilita el formulario personalizado
                .csrf(csrf -> csrf.disable())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .deleteCookies("JSESSIONID")
                        .invalidateHttpSession(true)
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.sendRedirect("/login?logout");
                        }))
                .sessionManagement(session -> session
                        .maximumSessions(10) // Opcional, pero obliga al uso de SessionRegistry
                        .sessionRegistry(sessionRegistry));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configura un conversor de Authorities para que Spring Security pueda leer las authorities del token
     * @return
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix(""); // No añadir prefijo extra si ya viene en el claim
        grantedAuthoritiesConverter.setAuthoritiesClaimName("authorities"); // Nombre del claim en el JWT

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }
}