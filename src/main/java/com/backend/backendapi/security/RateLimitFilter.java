package com.backend.backendapi.security;

import com.backend.backendapi.config.AppProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Rate limiting por IP y ruta con Bucket4j. Replica los limites de slowapi del
 * backend FastAPI. Los cubos viven en un cache acotado con expiracion para no
 * crecer indefinidamente.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int ONE_MINUTE_SECONDS = 60;

    private final AppProperties.RateLimit config;
    private final Cache<String, Bucket> buckets;

    public RateLimitFilter(AppProperties properties) {
        this.config = properties.rateLimit();
        this.buckets = Caffeine.newBuilder()
                .expireAfterAccess(Duration.ofHours(1))
                .maximumSize(config.maxBuckets())
                .build();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !config.enabled();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Rule rule = resolveRule(request.getMethod(), request.getRequestURI());
        if (rule != null) {
            String key = clientIp(request) + '|' + rule.name();
            Bucket bucket = buckets.get(key, ignored -> newBucket(rule.limit()));
            if (bucket != null && !bucket.tryConsume(1)) {
                writeTooManyRequests(response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private Bucket newBucket(int limit) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(limit)
                        .refillGreedy(limit, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    private Rule resolveRule(String method, String path) {
        if ("POST".equals(method)) {
            return switch (path) {
                case "/api/register" -> new Rule("register", config.registerPerMinute());
                case "/api/login" -> new Rule("login", config.loginPerMinute());
                case "/api/auth/google/exchange" -> new Rule("google-exchange", config.exchangePerMinute());
                case "/api/tasks" -> new Rule("task-create", config.taskWritePerMinute());
                default -> null;
            };
        }
        if ("PUT".equals(method) && isTaskByIdPath(path)) {
            return new Rule("task-update", config.taskWritePerMinute());
        }
        if ("DELETE".equals(method)) {
            if (isDeleteAllPath(path)) {
                return new Rule("tasks-delete-all", config.deleteAllPerMinute());
            }
            if (isTaskByIdPath(path)) {
                return new Rule("task-delete", config.taskWritePerMinute());
            }
        }
        return null;
    }

    private static boolean isTaskByIdPath(String path) {
        if (!path.startsWith("/api/tasks/")) {
            return false;
        }
        String rest = path.substring("/api/tasks/".length());
        return !rest.isEmpty() && rest.indexOf('/') < 0;
    }

    private static boolean isDeleteAllPath(String path) {
        return path.startsWith("/api/tasks/user/") && path.endsWith("/deleteall");
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            String first = forwarded.split(",")[0].trim();
            if (!first.isEmpty()) {
                return first;
            }
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(ONE_MINUTE_SECONDS));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"error\":\"Demasiadas peticiones\","
                        + "\"detail\":\"Has superado el límite de peticiones. Intenta de nuevo en un minuto.\"}");
    }

    private record Rule(String name, int limit) {
    }
}
