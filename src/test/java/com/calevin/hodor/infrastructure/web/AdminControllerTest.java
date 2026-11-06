package com.calevin.hodor.infrastructure.web;

import com.calevin.hodor.application.dtos.ClientRegistrationRequest;
import com.calevin.hodor.application.dtos.UserRegistrationRequest;
import com.calevin.hodor.application.services.ClientManagementService;
import com.calevin.hodor.application.services.UserAdminService;
import com.calevin.hodor.infrastructure.security.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;
import com.calevin.hodor.application.dtos.AuthorizedSystemResponse;
import com.calevin.hodor.application.dtos.ClientResponse;
import com.calevin.hodor.application.dtos.ClientUpdateRequest;
import com.calevin.hodor.application.dtos.UserResponse;
import com.calevin.hodor.application.exceptions.ClientNotFoundException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
class AdminControllerTest {

        @Autowired
        private MockMvc mockMvc;

        private final ObjectMapper objectMapper = new ObjectMapper();

        @MockitoBean
        private UserAdminService userAdminService;

        @MockitoBean
        private ClientManagementService clientManagementService;

        @MockitoBean
        private org.springframework.security.core.session.SessionRegistry sessionRegistry;

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Debe permitir registrar usuario si tiene rol ADMIN y el payload es valido")
        void registerUser_WhenValidPayloadAndAdmin_ReturnsCreated() throws Exception {
                UserRegistrationRequest request = new UserRegistrationRequest("newuser", "securepass123",
                                "blog-client");

                doNothing().when(userAdminService).registerUserForSystem(any());

                mockMvc.perform(post("/api/admin/users")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated());

                verify(userAdminService).registerUserForSystem(any(UserRegistrationRequest.class));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Debe devolver Forbidden (403) si el usuario NO tiene el rol ADMIN")
        void registerUser_WhenNotAdmin_ReturnsForbidden() throws Exception {
                UserRegistrationRequest request = new UserRegistrationRequest("newuser", "securepass123",
                                "blog-client");

                mockMvc.perform(post("/api/admin/users")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Debe redirigir al login (3xx) si el usuario no esta autenticado")
        void registerUser_WhenNotAuthenticated_ReturnsRedirect() throws Exception {
                UserRegistrationRequest request = new UserRegistrationRequest("newuser", "securepass123",
                                "blog-client");

                mockMvc.perform(post("/api/admin/users")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().is3xxRedirection());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Debe devolver Bad Request (400) cuando hay errores de validacion en la request")
        void registerUser_WhenInvalidPayload_ReturnsBadRequest() throws Exception {
                // Username is too short, passing an empty target_system
                UserRegistrationRequest request = new UserRegistrationRequest("usr", "pass", "");

                mockMvc.perform(post("/api/admin/users")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Debe permitir registrar un cliente si tiene rol ADMIN y el payload es valido")
        void registerClient_WhenValidPayloadAndAdmin_ReturnsCreated() throws Exception {
                java.util.Set<String> scopes = java.util.Set.of("openid", "profile");
                ClientRegistrationRequest request = new ClientRegistrationRequest("new-client", "secret123",
                                "http://localhost/callback", "http://localhost/logout", scopes, 15L, 7L);

                doNothing().when(clientManagementService).registerClient(any());

                mockMvc.perform(post("/api/admin/clients")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated());

                verify(clientManagementService).registerClient(any(ClientRegistrationRequest.class));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Debe permitir registrar un cliente sin timeouts (usar valores por defecto)")
        void registerClient_WhenTimeoutsMissing_ReturnsCreated() throws Exception {
                // Preparamos un request JSON sin los campos opcionales
                String json = """
                                {
                                  "clientId": "no-timeout-client",
                                  "clientSecret": "secret123",
                                  "redirectUri": "http://localhost/callback",
                                  "scopes": ["openid"]
                                }
                                """;

                doNothing().when(clientManagementService).registerClient(any());

                mockMvc.perform(post("/api/admin/clients")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                                .andExpect(status().isCreated());

                verify(clientManagementService).registerClient(any(ClientRegistrationRequest.class));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Debe devolver Bad Request (400) cuando el payload de registro de cliente es invalido")
        void registerClient_WhenInvalidPayload_ReturnsBadRequest() throws Exception {
                // Missing secret and empty scopes
                ClientRegistrationRequest request = new ClientRegistrationRequest("new-client", "",
                                "http://localhost/callback", null,
                                java.util.Set.of(), null, null);

                mockMvc.perform(post("/api/admin/clients")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Debe listar todos los usuarios si tiene rol ADMIN")
        void listUsers_WhenAdmin_ReturnsUserList() throws Exception {
                UserResponse user = new UserResponse(
                                UUID.randomUUID(),
                                "testuser",
                                true,
                                Set.of("ROLE_USER"),
                                List.of(new AuthorizedSystemResponse("blog-client", List.of("ROLE_AUTHOR"))));

                when(userAdminService.findAllUsers()).thenReturn(List.of(user));

                mockMvc.perform(get("/api/admin/users"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(1))
                                .andExpect(jsonPath("$[0].username").value("testuser"))
                                .andExpect(jsonPath("$[0].authorizedSystems[0].clientId").value("blog-client"))
                                .andExpect(jsonPath("$[0].authorizedSystems[0].roles[0]").value("ROLE_AUTHOR"));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Debe devolver Forbidden (403) al listar usuarios si NO tiene rol ADMIN")
        void listUsers_WhenNotAdmin_ReturnsForbidden() throws Exception {
                mockMvc.perform(get("/api/admin/users"))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Debe redirigir al login (3xx) al listar usuarios si no esta autenticado")
        void listUsers_WhenNotAuthenticated_ReturnsRedirect() throws Exception {
                mockMvc.perform(get("/api/admin/users"))
                                .andExpect(status().is3xxRedirection());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Debe actualizar el cliente y retornar 200 OK con el estado final si tiene rol ADMIN")
        void updateClient_WhenValidPayloadAndAdmin_ReturnsOk() throws Exception {
                String clientId = "service-client";
                ClientUpdateRequest request = new ClientUpdateRequest(
                                "https://new.domain.com/callback",
                                "https://new.domain.com/logout",
                                Set.of("openid", "profile"),
                                120L,
                                45L);

                ClientResponse response = new ClientResponse(
                                clientId,
                                "https://new.domain.com/callback",
                                "https://new.domain.com/logout",
                                Set.of("openid", "profile"),
                                120L,
                                45L);

                when(clientManagementService.updateClient(eq(clientId), any(ClientUpdateRequest.class)))
                                .thenReturn(response);

                mockMvc.perform(patch("/api/admin/clients/" + clientId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.clientId").value(clientId))
                                .andExpect(jsonPath("$.redirectUri").value("https://new.domain.com/callback"))
                                .andExpect(jsonPath("$.postLogoutRedirectUri").value("https://new.domain.com/logout"))
                                .andExpect(jsonPath("$.accessTokenTimeoutMinutes").value(120))
                                .andExpect(jsonPath("$.refreshTokenTimeoutDays").value(45))
                                .andExpect(jsonPath("$.clientSecret").doesNotExist());

                verify(clientManagementService).updateClient(eq(clientId), any(ClientUpdateRequest.class));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Debe devolver 404 Not Found si el cliente no existe")
        void updateClient_WhenClientNotFound_ReturnsNotFound() throws Exception {
                String clientId = "unknown-client";
                ClientUpdateRequest request = new ClientUpdateRequest(
                                "https://domain.com/callback",
                                null,
                                null,
                                null,
                                null);

                when(clientManagementService.updateClient(eq(clientId), any(ClientUpdateRequest.class)))
                                .thenThrow(new ClientNotFoundException("El cliente '" + clientId + "' no existe."));

                mockMvc.perform(patch("/api/admin/clients/" + clientId)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Debe devolver 400 Bad Request cuando el formato de URI o timeouts son invalidos")
        void updateClient_WhenInvalidPayload_ReturnsBadRequest() throws Exception {
                ClientUpdateRequest request = new ClientUpdateRequest(
                                "invalid-uri-not-http",
                                null,
                                null,
                                -5L,
                                null);

                mockMvc.perform(patch("/api/admin/clients/test-client")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Debe devolver Forbidden (403) al actualizar cliente si NO tiene rol ADMIN")
        void updateClient_SecurityChecks_WhenNotAdmin_ReturnsForbidden() throws Exception {
                ClientUpdateRequest request = new ClientUpdateRequest(
                                "https://domain.com/callback",
                                null,
                                null,
                                null,
                                null);

                mockMvc.perform(patch("/api/admin/clients/test-client")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("Debe redirigir al login (3xx) al actualizar cliente si no esta autenticado")
        void updateClient_SecurityChecks_WhenNotAuthenticated_ReturnsRedirectOrUnauthorized() throws Exception {
                ClientUpdateRequest request = new ClientUpdateRequest(
                                "https://domain.com/callback",
                                null,
                                null,
                                null,
                                null);

                mockMvc.perform(patch("/api/admin/clients/test-client")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().is3xxRedirection());
        }
}
