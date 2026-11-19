package com.backend.backendapi.security;

import com.backend.backendapi.config.AppProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(properties("secreto-principal-de-pruebas-suficientemente-largo"));

    @Test
    void createsTokenWithUsernameAsSubject() {
        String token = jwtService.createToken("demo@example.com");

        assertThat(token).isNotBlank();
        assertThat(jwtService.validateAndGetUsername(token)).contains("demo@example.com");
    }

    @Test
    void rejectsTamperedOrMalformedToken() {
        String token = jwtService.createToken("demo@example.com");

        assertThat(jwtService.validateAndGetUsername(token + "x")).isEmpty();
        assertThat(jwtService.validateAndGetUsername("no-es-un-jwt")).isEmpty();
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        JwtService other = new JwtService(properties("otro-secreto-de-pruebas-suficientemente-largo"));
        String token = other.createToken("otro@example.com");

        assertThat(other.validateAndGetUsername(token)).contains("otro@example.com");
        assertThat(jwtService.validateAndGetUsername(token)).isEmpty();
    }

    private static AppProperties properties(String secret) {
        return new AppProperties(
                "http://localhost:3000",
                new AppProperties.Cors(List.of("http://localhost:3000")),
                new AppProperties.Jwt(secret, 60),
                new AppProperties.AuthCode(5),
                new AppProperties.RateLimit(false, 5, 10, 10, 20, 5, 1000),
                new AppProperties.Google("", "", "http://localhost:8000/authorize/google"));
    }
}
