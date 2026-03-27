package com.calevin.hodor.infrastructure.web;

import com.calevin.hodor.application.dtos.ClientRegistrationRequest;
import com.calevin.hodor.application.dtos.UserRegistrationRequest;
import com.calevin.hodor.application.services.ClientManagementService;
import com.calevin.hodor.application.services.UserAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserAdminService userAdminService;
    private final ClientManagementService clientManagementService;

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')") // Solo usuarios con ROLE_ADMIN en el JWT
    public void registerUser(@Valid @RequestBody UserRegistrationRequest request) {
        userAdminService.registerUserForSystem(request);
    }

    @PostMapping("/clients")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public void registerClient(@Valid @RequestBody ClientRegistrationRequest request) {
        clientManagementService.registerClient(request);
    }
}