package com.backend.backendapi.security;

import com.backend.backendapi.config.AppProperties;
import com.backend.backendapi.dto.auth.AuthResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Tras validar el id_token de Google: crea/actualiza el usuario, emite el JWT,
 * genera el codigo temporal y redirige al callback del frontend.
 */
@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2SuccessHandler.class);

    private final AppProperties properties;
    private final com.backend.backendapi.service.AuthService authService;
    private final AuthCodeService authCodeService;

    public OAuth2SuccessHandler(AppProperties properties,
                                com.backend.backendapi.service.AuthService authService,
                                AuthCodeService authCodeService) {
        this.properties = properties;
        this.authService = authService;
        this.authCodeService = authCodeService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        if (!(authentication.getPrincipal() instanceof OidcUser oidcUser)
                || !StringUtils.hasText(oidcUser.getEmail())) {
            redirect(response, "no_email");
            return;
        }

        try {
            AuthResponse auth = authService.loginWithGoogle(oidcUser.getEmail());
            String code = authCodeService.create(auth.accessToken(), auth.user());
            String target = UriComponentsBuilder.fromUriString(properties.frontendUrl())
                    .path("/auth/google/callback")
                    .queryParam("code", code)
                    .build()
                    .toUriString();
            response.sendRedirect(target);
        } catch (RuntimeException ex) {
            log.error("Error creando el usuario de Google", ex);
            redirect(response, "auth_failed");
        }
    }

    private void redirect(HttpServletResponse response, String error) throws IOException {
        String target = UriComponentsBuilder.fromUriString(properties.frontendUrl())
                .path("/auth/google/callback")
                .queryParam("error", error)
                .build()
                .toUriString();
        response.sendRedirect(target);
    }
}
