package com.backend.backendapi.security;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Activa el registro de Google solo cuando ambas credenciales tienen valor.
 *
 * <p>Se evalua con el {@code Environment} en lugar de SpEL para tolerar
 * valores vacios y comillas provenientes de archivos {@code .env}.</p>
 */
public class GoogleCredentialsCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return hasText(context.getEnvironment().getProperty("app.google.client-id"))
                && hasText(context.getEnvironment().getProperty("app.google.client-secret"));
    }

    private static boolean hasText(String value) {
        if (value == null) {
            return false;
        }
        String candidate = value.strip();
        if (candidate.length() >= 2) {
            char first = candidate.charAt(0);
            char last = candidate.charAt(candidate.length() - 1);
            if ((first == '\'' || first == '"') && first == last) {
                candidate = candidate.substring(1, candidate.length() - 1).strip();
            }
        }
        return !candidate.isEmpty();
    }
}
