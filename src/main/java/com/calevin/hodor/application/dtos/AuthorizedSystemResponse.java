package com.calevin.hodor.application.dtos;

import java.util.List;

/**
 * Representa el acceso de un usuario a un sistema específico con sus roles correspondientes.
 */
public record AuthorizedSystemResponse(
    String clientId,
    List<String> roles
) {
}
