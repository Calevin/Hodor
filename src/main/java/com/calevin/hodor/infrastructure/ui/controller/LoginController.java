package com.calevin.hodor.infrastructure.ui.controller;

import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;

/**
 * Controlador de la UI para gestionar la página de inicio de sesión personalizada.
 * 
 * @author Hodor Agent
 */
@Controller
public class LoginController {

    private final RegisteredClientRepository registeredClientRepository;
    private final RequestCache requestCache = new HttpSessionRequestCache();

    public LoginController(RegisteredClientRepository registeredClientRepository) {
        this.registeredClientRepository = registeredClientRepository;
    }

    /**
     * Sirve la vista de inicio de sesión (Thymeleaf).
     * 
     * @param error Si se incluye el parámetro error, indica que hubo un intento fallido.
     * @param logout Si se incluye el parámetro logout, indica que el usuario acaba de salir.
     * @param request La petición HTTP actual.
     * @param response La respuesta HTTP actual.
     * @param model El modelo de Spring MVC para pasar datos a la vista.
     * @return El nombre de la plantilla (login.html).
     */
    @GetMapping("/login")
    public String login(
            @RequestParam(name = "error", required = false) String error,
            @RequestParam(name = "logout", required = false) String logout,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model) {

        // Intentamos recuperar la aplicación que solicita el login desde la RequestCache
        SavedRequest savedRequest = requestCache.getRequest(request, response);
        if (savedRequest != null) {
            String[] clientIds = savedRequest.getParameterValues("client_id");
            if (clientIds != null && clientIds.length > 0) {
                String clientId = clientIds[0];
                RegisteredClient client = registeredClientRepository.findByClientId(clientId);
                if (client != null) {
                    // Preferimos mostrar el clientId (ej. 'hodor-blog') si clientName es nulo o parece
                    // ser el ID interno (un UUID de 36 caracteres).
                    String clientName = client.getClientName();
                    String clientIdValue = client.getClientId();
                    
                    String displayName = (clientName != null && !clientName.isBlank() && clientName.length() != 36) 
                            ? clientName 
                            : clientIdValue;
                    model.addAttribute("clientName", displayName);
                }
            }
        }

        model.addAttribute("hasError", Optional.ofNullable(error).isPresent());
        model.addAttribute("isLogout", Optional.ofNullable(logout).isPresent());

        return "login";
    }
}
