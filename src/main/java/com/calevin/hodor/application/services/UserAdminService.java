package com.calevin.hodor.application.services;

import com.calevin.hodor.application.dtos.UserRegistrationRequest;
import com.calevin.hodor.application.dtos.UserResponse;
import com.calevin.hodor.infrastructure.persistence.entities.AuthorityEntity;
import com.calevin.hodor.infrastructure.persistence.entities.UserEntity;
import com.calevin.hodor.infrastructure.persistence.repositories.UserRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserRepository userRepository;
    private final RegisteredClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Busca el cliente, hashea la contraseña y persiste la relación.
     * @param request
     */
    @Transactional
    public void registerUserForSystem(UserRegistrationRequest request) {
        // 1. Validar que el sistema destino exista
        RegisteredClient client = clientRepository.findByClientId(request.target_system());
        if (client == null) {
            throw new IllegalArgumentException("El sistema '" + request.target_system() + "' no está registrado.");
        }

        // 2. Prevenir duplicados de usuario
        if (userRepository.findByUsername(request.username()).isPresent()) {
            throw new IllegalStateException("El nombre de usuario ya existe.");
        }

        // 3. Crear el usuario
        var user = UserEntity.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .enabled(true)
                .build();

        // Asignamos un rol básico por defecto
        user.addAuthority("ROLE_USER");

        // Al guardar, JPA asigna el UUID generado a la instancia 'user'
        userRepository.save(user);

        // 4. Persistir vínculo en tabla intermedia
        // Usamos el ID interno de Spring Auth Server (client.getId()) 
        // y el UUID del usuario recién creado.
        userRepository.linkUserToClient(user.getId(), client.getId());
    }

    @Transactional(readOnly = true)
    public List<UserResponse> findAllUsers() {
        return userRepository.findAll().stream()
                .map(user -> {
                    List<String> systems = userRepository.findAuthorizedClientsByUsername(user.getUsername());
                    return new UserResponse(
                            user.getId(),
                            user.getUsername(),
                            user.isEnabled(),
                            user.getAuthorities().stream()
                                    .map(AuthorityEntity::getAuthority)
                                    .collect(Collectors.toSet()),
                            systems);
                })
                .toList();
    }
}