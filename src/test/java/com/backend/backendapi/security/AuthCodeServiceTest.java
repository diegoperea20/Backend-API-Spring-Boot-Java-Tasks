package com.backend.backendapi.security;

import com.backend.backendapi.config.AppProperties;
import com.backend.backendapi.dto.auth.UserDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AuthCodeServiceTest {

    private final AuthCodeService authCodeService = new AuthCodeService(properties());

    @Test
    void codeCanOnlyBeConsumedOnce() {
        UserDto user = new UserDto(1L, "demo@example.com", LocalDateTime.now(), LocalDateTime.now());

        String code = authCodeService.create("jwt-token", user);

        assertThat(authCodeService.consume(code)).isPresent()
                .get()
                .satisfies(stored -> {
                    assertThat(stored.token()).isEqualTo("jwt-token");
                    assertThat(stored.user().username()).isEqualTo("demo@example.com");
                });
        assertThat(authCodeService.consume(code)).isEmpty();
    }

    @Test
    void unknownCodeIsRejected() {
        assertThat(authCodeService.consume("codigo-inexistente")).isEmpty();
    }

    private static AppProperties properties() {
        return new AppProperties(
                "http://localhost:3000",
                new AppProperties.Cors(List.of("http://localhost:3000")),
                new AppProperties.Jwt("un-secreto-de-pruebas-suficientemente-largo", 60),
                new AppProperties.AuthCode(5),
                new AppProperties.RateLimit(false, 5, 10, 10, 20, 5, 1000),
                new AppProperties.Google("", "", "http://localhost:8000/authorize/google"));
    }
}
