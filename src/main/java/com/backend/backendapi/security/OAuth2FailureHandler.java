package com.backend.backendapi.security;

import com.backend.backendapi.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2FailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2FailureHandler.class);

    private final AppProperties properties;

    public OAuth2FailureHandler(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        log.warn("Fallo la autenticacion con Google: {}", exception.getMessage());
        String target = UriComponentsBuilder.fromUriString(properties.frontendUrl())
                .path("/auth/google/callback")
                .queryParam("error", "auth_failed")
                .build()
                .toUriString();
        response.sendRedirect(target);
    }
}
