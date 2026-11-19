package com.backend.backendapi.web;

import com.backend.backendapi.config.AppProperties;
import com.backend.backendapi.dto.auth.AuthResponse;
import com.backend.backendapi.dto.auth.ExchangeRequest;
import com.backend.backendapi.dto.auth.HealthResponse;
import com.backend.backendapi.dto.auth.LoginRequest;
import com.backend.backendapi.dto.auth.ProfileResponse;
import com.backend.backendapi.dto.auth.ProtectedResponse;
import com.backend.backendapi.dto.auth.RegisterRequest;
import com.backend.backendapi.dto.auth.StatsResponse;
import com.backend.backendapi.repository.UserRepository;
import com.backend.backendapi.security.AuthCodeService;
import com.backend.backendapi.security.AuthenticatedUser;
import com.backend.backendapi.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final AuthCodeService authCodeService;
    private final JdbcTemplate jdbcTemplate;
    private final AppProperties properties;

    public AuthController(AuthService authService, UserRepository userRepository,
                          AuthCodeService authCodeService, JdbcTemplate jdbcTemplate,
                          AppProperties properties) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.authCodeService = authCodeService;
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        String database;
        try {
            jdbcTemplate.queryForObject("select 1", Integer.class);
            database = "connected";
        } catch (DataAccessException ex) {
            database = "disconnected";
        }
        return new HealthResponse("ok", "API Spring Boot funcionando correctamente", database, utcNow());
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request.username(), request.password());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.username(), request.password());
    }

    /**
     * Alias compatible con el contrato del backend FastAPI. Spring Security
     * anade "/{registrationId}" a la base URI del endpoint de autorizacion, por
     * lo que la URL real es /api/oauth2/authorization/google.
     */
    @GetMapping("/login/google")
    public void googleLogin(HttpServletResponse response) throws IOException {
        response.sendRedirect("/api/oauth2/authorization/google");
    }

    /**
     * Solo se ejecuta cuando Google no esta configurado: si hay credenciales, el
     * filtro de OAuth2 intercepta esta ruta antes de llegar al controlador.
     */
    @GetMapping("/oauth2/authorization/google")
    public void googleLoginFallback(HttpServletResponse response) throws IOException {
        String target = UriComponentsBuilder.fromUriString(properties.frontendUrl())
                .path("/auth/google/callback")
                .queryParam("error", "auth_failed")
                .build()
                .toUriString();
        response.sendRedirect(target);
    }

    @PostMapping("/auth/google/exchange")
    public AuthResponse exchange(@Valid @RequestBody ExchangeRequest request) {
        return authService.exchangeCode(request.code());
    }

    @GetMapping("/protected")
    public ProtectedResponse protectedRoute(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return new ProtectedResponse("Acceso autorizado", currentUser.toDto());
    }

    @GetMapping("/user/profile")
    public ProfileResponse profile(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return new ProfileResponse(currentUser.toDto());
    }

    @GetMapping("/stats")
    public StatsResponse stats() {
        return new StatsResponse(userRepository.count(), authCodeService.activeCount(), utcNow());
    }

    private static LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
