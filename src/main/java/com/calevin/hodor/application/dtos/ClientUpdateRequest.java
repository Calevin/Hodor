package com.calevin.hodor.application.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.Set;

/**
 * DTO para la actualizacion parcial de configuracion de un cliente OAuth2.
 */
public record ClientUpdateRequest(
        @Pattern(regexp = "^https?://.+", message = "redirectUri must be a valid HTTP/HTTPS URL")
        String redirectUri,

        @Pattern(regexp = "^(https?://.+)?$", message = "postLogoutRedirectUri must be a valid HTTP/HTTPS URL or empty string")
        String postLogoutRedirectUri,

        @Size(min = 1, message = "scopes must not be empty if provided")
        Set<@NotBlank String> scopes,

        @Positive(message = "accessTokenTimeoutMinutes must be greater than zero")
        Long accessTokenTimeoutMinutes,

        @Positive(message = "refreshTokenTimeoutDays must be greater than zero")
        Long refreshTokenTimeoutDays
) {
}
