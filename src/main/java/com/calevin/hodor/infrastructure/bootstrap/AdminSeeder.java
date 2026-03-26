package com.calevin.hodor.infrastructure.bootstrap;

import com.calevin.hodor.infrastructure.persistence.entities.UserEntity;
import com.calevin.hodor.infrastructure.persistence.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${hodor.admin.username}")
    private String adminUsername;

    @Value("${hodor.admin.password}")
    private String adminPassword;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seedAdminUser() {
        log.info("Verificando existencia de administrador primario...");

        if (userRepository.existsByAuthorities_Authority("ROLE_ADMIN")) {
            log.info("Administrador ya presente en el sistema. Omitiendo inicialización.");
            return;
        }

        validateEnvironmentVariables();

        var adminUser = UserEntity.builder()
                .username(adminUsername)
                .password(passwordEncoder.encode(adminPassword))
                .enabled(true)
                .build();

        adminUser.addAuthority("ROLE_ADMIN");

        userRepository.save(adminUser);
        log.info("Hodor: Primer administrador '{}' creado exitosamente.", adminUsername);
    }

    private void validateEnvironmentVariables() {
        if (adminUsername.isBlank() || adminPassword.isBlank()) {
            log.error(
                    "ERROR CRÍTICO: Las variables AUTH_ADMIN_USER y AUTH_ADMIN_PASS son obligatorias para el primer inicio.");
            log.error("El sistema no puede garantizar el acceso administrativo. Abortando...");
            System.exit(1); // Fail-fast mechanism
        }
    }
}