package com.calevin.hodor.infrastructure.security;

import com.calevin.hodor.infrastructure.persistence.entities.AuthorityEntity;
import com.calevin.hodor.infrastructure.persistence.entities.UserEntity;
import com.calevin.hodor.infrastructure.persistence.repositories.UserRepository;
import com.calevin.hodor.infrastructure.security.config.JwtTokenCustomizerConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityInfrastructureTest {

    @Nested
    @DisplayName("Tests para UserDetailsServiceImpl")
    class UserDetailsServiceImplTests {
        @Mock
        private UserRepository userRepository;
        @InjectMocks
        private UserDetailsServiceImpl userDetailsService;

        @Test
        @DisplayName("Debe cargar un usuario por nombre correctamente")
        void loadUserByUsername_Success() {
            UserEntity user = UserEntity.builder()
                    .username("test")
                    .password("secret")
                    .enabled(true)
                    .authorities(Set.of(AuthorityEntity.builder().authority("ROLE_USER").build()))
                    .build();

            when(userRepository.findByUsername("test")).thenReturn(Optional.of(user));

            UserDetails userDetails = userDetailsService.loadUserByUsername("test");

            assertThat(userDetails.getUsername()).isEqualTo("test");
            assertThat(userDetails.getAuthorities()).hasSize(1)
                    .extracting("authority")
                    .containsExactly("ROLE_USER");
        }

        @Test
        @DisplayName("Debe fallar si el usuario no existe")
        void loadUserByUsername_NotFound() {
            when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("Usuario no encontrado");
        }
    }

    @Nested
    @DisplayName("Tests para JwtTokenCustomizerConfig")
    class JwtTokenCustomizerConfigTests {
        @Mock
        private UserRepository userRepository;
        
        @Test
        @DisplayName("Debe inyectar claims adicionales en el Access Token")
        void jwtTokenCustomizer_AddsCustomClaims() {
            JwtTokenCustomizerConfig config = new JwtTokenCustomizerConfig(userRepository);
            JwtEncodingContext context = mock(JwtEncodingContext.class);
            JwtClaimsSet.Builder claimBuilder = JwtClaimsSet.builder();

            when(context.getTokenType()).thenReturn(OAuth2TokenType.ACCESS_TOKEN);
            when(context.getClaims()).thenReturn(claimBuilder);
            
            UsernamePasswordAuthenticationToken principal = new UsernamePasswordAuthenticationToken(
                "user", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
            when(context.getPrincipal()).thenReturn(principal);
            when(userRepository.findAuthorizedClientsByUsername("user")).thenReturn(List.of("blog", "api"));

            config.jwtTokenCustomizer().customize(context);

            JwtClaimsSet finalClaims = claimBuilder.build();
            assertThat(finalClaims.getClaims()).containsEntry("authorized_systems", List.of("blog", "api"));
            assertThat(finalClaims.getClaims()).containsEntry("roles", List.of("ROLE_ADMIN"));
        }

        @Test
        @DisplayName("No debe añadir claims si no es un Access Token")
        void jwtTokenCustomizer_DoesNothingForNonAccessToken() {
            JwtTokenCustomizerConfig config = new JwtTokenCustomizerConfig(userRepository);
            JwtEncodingContext context = mock(JwtEncodingContext.class);

            when(context.getTokenType()).thenReturn(new OAuth2TokenType("other"));

            config.jwtTokenCustomizer().customize(context);

            verify(context, never()).getClaims();
            verify(userRepository, never()).findAuthorizedClientsByUsername(anyString());
        }
    }
}
