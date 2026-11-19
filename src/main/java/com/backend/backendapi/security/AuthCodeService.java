package com.backend.backendapi.security;

import com.backend.backendapi.config.AppProperties;
import com.backend.backendapi.dto.auth.UserDto;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

/**
 * Codigos temporales de un solo uso que el callback de Google entrega al
 * frontend. Equivalente al diccionario en memoria de auth.py, pero con
 * expiracion y tamano maximo gestionados por Caffeine.
 */
@Service
public class AuthCodeService {

    public record StoredCode(String token, UserDto user) {
    }

    private final Cache<String, StoredCode> codes;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthCodeService(AppProperties properties) {
        this.codes = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(properties.authCode().ttlMinutes()))
                .maximumSize(10_000)
                .build();
    }

    public String create(String token, UserDto user) {
        String code = newCode();
        codes.put(code, new StoredCode(token, user));
        return code;
    }

    /**
     * Recupera y elimina el codigo de forma atomica (un solo uso).
     */
    public Optional<StoredCode> consume(String code) {
        return Optional.ofNullable(codes.asMap().remove(code));
    }

    public long activeCount() {
        codes.cleanUp();
        return codes.estimatedSize();
    }

    private String newCode() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
