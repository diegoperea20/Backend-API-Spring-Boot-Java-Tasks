package com.backend.backendapi.service;

import com.backend.backendapi.domain.User;
import com.backend.backendapi.dto.auth.AuthResponse;
import com.backend.backendapi.dto.auth.UserDto;
import com.backend.backendapi.repository.UserRepository;
import com.backend.backendapi.security.AuthCodeService;
import com.backend.backendapi.security.JwtService;
import com.backend.backendapi.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthCodeService authCodeService;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder,
                       JwtService jwtService, AuthCodeService authCodeService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authCodeService = authCodeService;
    }

    @Transactional
    public AuthResponse register(String username, String password) {
        if (username.length() < 3) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El username debe tener al menos 3 caracteres");
        }
        if (password.length() < 6) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La contraseña debe tener al menos 6 caracteres");
        }
        if (users.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "El username ya existe");
        }

        User user = new User(username, passwordEncoder.encode(password));
        user.setLastLogin(utcNow());
        try {
            user = users.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(HttpStatus.CONFLICT, "El username ya existe");
        }

        log.info("Usuario registrado exitosamente: {}", username);
        return new AuthResponse("Usuario registrado exitosamente",
                jwtService.createToken(user.getUsername()), UserDto.from(user));
    }

    @Transactional
    public AuthResponse login(String username, String password) {
        User user = users.findByUsername(username).orElseThrow(this::invalidCredentials);
        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw invalidCredentials();
        }

        user.setLastLogin(utcNow());
        users.saveAndFlush(user);

        log.info("Login exitoso: {}", username);
        return new AuthResponse("Login exitoso",
                jwtService.createToken(user.getUsername()), UserDto.from(user));
    }

    @Transactional
    public AuthResponse loginWithGoogle(String email) {
        User user = users.findByUsername(email).orElseGet(() -> new User(email, null));
        user.setLastLogin(utcNow());
        user = users.saveAndFlush(user);

        log.info("Login con Google exitoso: {}", email);
        return new AuthResponse("Autenticación con Google exitosa",
                jwtService.createToken(user.getUsername()), UserDto.from(user));
    }

    public AuthResponse exchangeCode(String code) {
        AuthCodeService.StoredCode stored = authCodeService.consume(code)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Código inválido o expirado"));
        return new AuthResponse("Autenticación con Google exitosa", stored.token(), stored.user());
    }

    private ApiException invalidCredentials() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "Username o password inválidos");
    }

    private static LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
