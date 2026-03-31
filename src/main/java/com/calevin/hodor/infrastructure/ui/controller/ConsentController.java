package com.calevin.hodor.infrastructure.ui.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controlador para gestionar la página de consentimiento personalizada de OAuth2.
 * 
 * @author Hodor Agent
 */
@Controller
@RequiredArgsConstructor
public class ConsentController {

    private final RegisteredClientRepository registeredClientRepository;
    private final OAuth2AuthorizationConsentService authorizationConsentService;

    /**
     * Prepara y sirve la vista de consentimiento.
     * 
     * @param principal El usuario autenticado actual.
     * @param model Modelo para la vista.
     * @param clientId ID del cliente que solicita la autorización.
     * @param scope Lista de scopes solicitados.
     * @param state Estado de la petición OAuth2.
     * @return El nombre de la plantilla (consent.html).
     */
    @GetMapping("/oauth2/consent")
    public String consent(Principal principal, Model model,
            @RequestParam(OAuth2ParameterNames.CLIENT_ID) String clientId,
            @RequestParam(OAuth2ParameterNames.SCOPE) String scope,
            @RequestParam(OAuth2ParameterNames.STATE) String state) {

        // 1. Buscamos los detalles del cliente
        RegisteredClient registeredClient = this.registeredClientRepository.findByClientId(clientId);
        if (registeredClient == null) {
            throw new IllegalArgumentException("Cliente no encontrado: " + clientId);
        }

        // Usamos el nombre amigable si no es un UUID, de lo contrario usamos el clientId
        String clientName = registeredClient.getClientName();
        String displayName = (clientName != null && !clientName.isBlank() && clientName.length() != 36)
                ? clientName
                : registeredClient.getClientId();

        // 2. Buscamos si el usuario ya ha dado consentimiento previo a algunos scopes
        OAuth2AuthorizationConsent currentConsent = this.authorizationConsentService.findById(registeredClient.getId(),
                principal.getName());

        Set<String> scopesToAuthorize = new HashSet<>();
        Set<String> previouslyAuthorizedScopes = new HashSet<>();

        // OAuth2 separa los scopes por espacios
        String[] requestedScopes = StringUtils.delimitedListToStringArray(scope, " ");
        for (String requestedScope : requestedScopes) {
            if (StringUtils.hasText(requestedScope)) {
                if (currentConsent != null && currentConsent.getScopes().contains(requestedScope)) {
                    previouslyAuthorizedScopes.add(requestedScope);
                } else {
                    scopesToAuthorize.add(requestedScope);
                }
            }
        }

        // 3. Pasamos los datos al modelo
        model.addAttribute("clientId", clientId);
        model.addAttribute("clientName", displayName);
        model.addAttribute("state", state);
        model.addAttribute("scopes", withDescription(scopesToAuthorize));
        model.addAttribute("previouslyAuthorizedScopes", withDescription(previouslyAuthorizedScopes));
        model.addAttribute("principalName", principal.getName());

        return "consent";
    }

    /**
     * Mapea un set de scopes a una lista de objetos ScopeInfo (usando records de Java).
     */
    private Set<ScopeInfo> withDescription(Set<String> scopes) {
        if (scopes == null)
            return Collections.emptySet();
        return scopes.stream()
                .map(s -> new ScopeInfo(s, "scope." + s))
                .collect(Collectors.toSet());
    }

    /**
     * Record para representar la información de un scope en la vista.
     */
    public record ScopeInfo(String scope, String descriptionKey) {
    }
}
