package com.backend.backendapi.security;

import com.backend.backendapi.config.AppProperties;
import com.backend.backendapi.config.AppProperties.Google;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;

/**
 * Registro de Google construido explicitamente (sin discovery en el arranque y
 * sin depender de spring.security.oauth2.client.*). Solo se crea si hay
 * credenciales, de forma que la aplicacion arranca sin ellas.
 */
@Configuration
public class GoogleClientConfig {

    @Bean
    @Conditional(GoogleCredentialsCondition.class)
    ClientRegistrationRepository googleClientRegistrationRepository(AppProperties properties) {
        Google google = properties.google();
        ClientRegistration registration = ClientRegistration.withRegistrationId("google")
                .clientId(google.clientId())
                .clientSecret(google.clientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(google.redirectUri())
                .scope("openid", "profile", "email")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .issuerUri("https://accounts.google.com")
                .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .clientName("Google")
                .build();

        return new InMemoryClientRegistrationRepository(registration);
    }
}
