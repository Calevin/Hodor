package com.calevin.hodor.application.dtos;

import jakarta.validation.constraints.NotBlank;

/**
 * Petición para asignar un rol específico a un usuario en un cliente/aplicación dada.
 */
public record RoleAssignmentRequest(
    @NotBlank String clientId, // Logical clientId (ej: 'blog-app')
    @NotBlank String role      // Role string (ej: 'ROLE_EDITOR')
) {
}
