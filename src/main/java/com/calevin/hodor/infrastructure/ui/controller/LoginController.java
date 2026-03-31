package com.calevin.hodor.infrastructure.ui.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

/**
 * Controlador de la UI para gestionar la página de inicio de sesión personalizada.
 * 
 * @author Hodor Agent
 */
@Controller
public class LoginController {

    /**
     * Sirve la vista de inicio de sesión (Thymeleaf).
     * 
     * @param error Si se incluye el parámetro error, indica que hubo un intento fallido.
     * @param logout Si se incluye el parámetro logout, indica que el usuario acaba de salir.
     * @param model El modelo de Spring MVC para pasar datos a la vista.
     * @return El nombre de la plantilla (login.html).
     */
    @GetMapping("/login")
    public String login(
            @RequestParam(name = "error", required = false) String error,
            @RequestParam(name = "logout", required = false) String logout,
            Model model) {

        // Usamos Optional (Java 8+) y podríamos usar Records si tuviéramos que enviar objetos complejos.
        // Para este caso simple, pasamos indicadores booleanos.
        model.addAttribute("hasError", Optional.ofNullable(error).isPresent());
        model.addAttribute("isLogout", Optional.ofNullable(logout).isPresent());

        return "login";
    }
}
