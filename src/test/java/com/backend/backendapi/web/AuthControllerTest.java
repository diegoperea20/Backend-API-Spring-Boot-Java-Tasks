package com.backend.backendapi.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthReportsDatabaseConnection() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.database").value("connected"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void registerReturnsTokenAndProfileWorks() throws Exception {
        String username = uniqueUsername();

        String token = register(username);

        mockMvc.perform(get("/api/user/profile").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value(username))
                .andExpect(jsonPath("$.user.id").isNumber())
                .andExpect(jsonPath("$.user.created_at").isNotEmpty())
                .andExpect(jsonPath("$.user.last_login").isNotEmpty());
    }

    @Test
    void loginReturnsToken() throws Exception {
        String username = uniqueUsername();
        register(username);

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, "secret123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login exitoso"))
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andExpect(jsonPath("$.user.username").value(username));
    }

    @Test
    void duplicateUsernameReturnsConflictWithErrorField() throws Exception {
        String username = uniqueUsername();
        register(username);

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, "secret123")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("El username ya existe"))
                .andExpect(jsonPath("$.detail").value("El username ya existe"));
    }

    @Test
    void wrongPasswordReturnsUnauthorized() throws Exception {
        String username = uniqueUsername();
        register(username);

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, "otra-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Username o password inválidos"));
    }

    @Test
    void shortCredentialsAreRejected() throws Exception {
        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("ab", "123456")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El username debe tener al menos 3 caracteres"));

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(uniqueUsername(), "123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La contraseña debe tener al menos 6 caracteres"));
    }

    @Test
    void protectedRouteRequiresBearerToken() throws Exception {
        mockMvc.perform(get("/api/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.error").value("No autorizado"))
                .andExpect(jsonPath("$.detail").value("Token inválido o ausente"));
    }

    @Test
    void protectedRouteReturnsAuthenticatedUser() throws Exception {
        String username = uniqueUsername();
        String token = register(username);

        mockMvc.perform(get("/api/protected").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Acceso autorizado"))
                .andExpect(jsonPath("$.user.username").value(username));
    }

    @Test
    void statsCountsUsers() throws Exception {
        String token = register(uniqueUsername());

        mockMvc.perform(get("/api/stats").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total_users").isNumber())
                .andExpect(jsonPath("$.active_auth_codes").isNumber())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void invalidExchangeCodeReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/google/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"codigo-invalido\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Código inválido o expirado"));
    }

    private String register(String username) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(username, "secret123")))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.access_token");
    }

    private static String credentials(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    private static String uniqueUsername() {
        return "auth-" + UUID.randomUUID();
    }
}
