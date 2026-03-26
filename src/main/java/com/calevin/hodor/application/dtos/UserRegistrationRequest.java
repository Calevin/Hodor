package com.calevin.hodor.application.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserRegistrationRequest(
        @NotBlank @Size(min = 4, max = 50) String username,
        @NotBlank @Size(min = 8) String password,
        @NotBlank String target_system // El client_id (ej. 'blog-client')
) {
}