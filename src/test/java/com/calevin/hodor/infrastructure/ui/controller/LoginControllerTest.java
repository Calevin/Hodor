package com.calevin.hodor.infrastructure.ui.controller;

import com.calevin.hodor.infrastructure.security.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Pruebas unitarias de capa web para LoginController y transiciones de sesion.
 */
@WebMvcTest(LoginController.class)
@Import(SecurityConfig.class)
class LoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisteredClientRepository registeredClientRepository;

    @MockitoBean
    private SessionRegistry sessionRegistry;

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("CHK-01: Usuario autenticado ve tarjeta de sesion activa y su nombre de usuario en lugar de campos de credenciales")
    void login_WhenAuthenticated_ShowsActiveSessionView() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("isAuthenticated", true))
                .andExpect(model().attribute("username", "admin"))
                .andExpect(content().string(containsString("admin")))
                .andExpect(content().string(not(containsString("id=\"password\""))));
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("CHK-02: Usuario autenticado dispone de un formulario POST hacia /logout")
    void login_WhenAuthenticated_ContainsLogoutPostForm() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/logout\"")))
                .andExpect(content().string(containsString("method=\"post\"")));
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("CHK-03: Usuario autenticado no visualiza controles redundantes de sesion")
    void login_WhenAuthenticated_DoesNotContainRedundantControls() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("switch-account-form"))));
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("CHK-04: Cierre de sesion ejecuta POST /logout y redirige a /login?logout")
    void logout_WhenTriggered_InvalidatesAndRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/logout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout"));
    }

    @Test
    @DisplayName("CHK-05: Usuario anonimo en /login ve formulario con usuario y contrasenia")
    void login_WhenAnonymous_ShowsCredentialsForm() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("isAuthenticated", false))
                .andExpect(content().string(containsString("id=\"username\"")))
                .andExpect(content().string(containsString("id=\"password\"")));
    }

    @Test
    @DisplayName("CHK-06: Usuario anonimo que accede a la raiz / es redirigido con 302 a /login")
    void root_WhenAnonymous_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("CHK-07: Usuario autenticado que accede a la raiz / es redirigido con 302 a /login")
    void root_WhenAuthenticated_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @WithMockUser(username = "admin")
    @DisplayName("CHK-08: Usuario autenticado con SavedRequest retenido ve opcion para continuar flujo a la aplicacion")
    void login_WhenAuthenticatedWithSavedRequest_ShowsContinueOption() throws Exception {
        String clientId = "hodor-notes";
        RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientName("Notas App")
                .authorizationGrantType(org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost:3000/callback")
                .build();

        when(registeredClientRepository.findByClientId(clientId)).thenReturn(client);

        SavedRequest mockSavedRequest = new SavedRequest() {
            @Override
            public String getRedirectUrl() {
                return "http://localhost:3000/callback";
            }

            @Override
            public List<Cookie> getCookies() {
                return List.of();
            }

            @Override
            public String getMethod() {
                return "GET";
            }

            @Override
            public List<String> getHeaderNames() {
                return List.of();
            }

            @Override
            public List<String> getHeaderValues(String name) {
                return List.of();
            }

            @Override
            public List<Locale> getLocales() {
                return List.of();
            }

            @Override
            public String[] getParameterValues(String name) {
                if ("client_id".equals(name)) {
                    return new String[]{clientId};
                }
                return null;
            }

            @Override
            public Map<String, String[]> getParameterMap() {
                return Map.of("client_id", new String[]{clientId});
            }
        };

        mockMvc.perform(get("/login").sessionAttr("SPRING_SECURITY_SAVED_REQUEST", mockSavedRequest))
                .andExpect(status().isOk())
                .andExpect(model().attribute("continueUrl", "http://localhost:3000/callback"))
                .andExpect(content().string(containsString("http://localhost:3000/callback")))
                .andExpect(content().string(containsString("Notas App")));
    }
}
