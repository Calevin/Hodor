package com.calevin.hodor.application.dtos;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        boolean enabled,
        Set<String> authorities,
        List<AuthorizedSystemResponse> authorizedSystems) {
}