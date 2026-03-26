package com.calevin.hodor.infrastructure.web;

import com.calevin.hodor.application.dtos.UserRegistrationRequest;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserAdminService userAdminService;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Debe permitir registrar usuario si tiene rol ADMIN y el payload es valido")
    void registerUser_WhenValidPayloadAndAdmin_ReturnsCreated() throws Exception {
        UserRegistrationRequest request = new UserRegistrationRequest("newuser", "securepass123", "blog-client");

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
        UserRegistrationRequest request = new UserRegistrationRequest("newuser", "securepass123", "blog-client");

        mockMvc.perform(post("/api/admin/users")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Debe redirigir al login (3xx) si el usuario no esta autenticado")
    void registerUser_WhenNotAuthenticated_ReturnsRedirect() throws Exception {
        UserRegistrationRequest request = new UserRegistrationRequest("newuser", "securepass123", "blog-client");

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
}
