package com.calevin.hodor.application.dtos;

import java.util.Set;

/**
 * DTO de respuesta que expone la configuracion consolidada del cliente sin revelar secretos.
 */
public record ClientResponse(
        String clientId,
        String redirectUri,
        String postLogoutRedirectUri,
        Set<String> scopes,
        Long accessTokenTimeoutMinutes,
        Long refreshTokenTimeoutDays
) {
}
