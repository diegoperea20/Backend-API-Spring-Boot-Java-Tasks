package com.backend.backendapi.security;

import com.backend.backendapi.config.AppProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Guarda el state de OAuth2 en memoria (con TTL) en lugar de la HttpSession.
 * Asi el flujo funciona cuando el login pasa por el proxy de Next.js: la cookie
 * de sesion quedaria en localhost:3000 pero el callback llega directo a :8000.
 * El state sigue protegiendo contra CSRF porque es aleatorio y de un solo uso.
 */
@Component
public class InMemoryAuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private final Cache<String, OAuth2AuthorizationRequest> requests;

    public InMemoryAuthorizationRequestRepository(AppProperties properties) {
        this.requests = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(properties.authCode().ttlMinutes()))
                .maximumSize(10_000)
                .build();
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        String state = request.getParameter(OAuth2ParameterNames.STATE);
        return state == null ? null : requests.getIfPresent(state);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request, HttpServletResponse response) {
        if (authorizationRequest == null) {
            removeAuthorizationRequest(request, response);
            return;
        }
        requests.put(authorizationRequest.getState(), authorizationRequest);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                 HttpServletResponse response) {
        String state = request.getParameter(OAuth2ParameterNames.STATE);
        return state == null ? null : requests.asMap().remove(state);
    }
}
