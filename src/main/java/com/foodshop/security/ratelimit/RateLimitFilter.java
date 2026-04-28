package com.foodshop.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodshop.dto.ApiResponse;
import com.foodshop.exception.GlobalCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {
    private static final RateLimitRule LOGIN_RULE =
            new RateLimitRule("auth-login", 5, Duration.ofMinutes(1), false);
    private static final RateLimitRule REGISTER_RULE =
            new RateLimitRule("auth-register", 5, Duration.ofMinutes(1), false);
    private static final RateLimitRule REFRESH_TOKEN_RULE =
            new RateLimitRule("auth-refresh-token", 10, Duration.ofMinutes(1), true);
    private static final RateLimitRule PUBLIC_SEARCH_RULE =
            new RateLimitRule("public-search", 60, Duration.ofMinutes(1), true);
    private static final RateLimitRule SENSITIVE_MUTATE_RULE =
            new RateLimitRule("sensitive-mutate", 30, Duration.ofMinutes(1), true);

    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        RateLimitRule rule = resolveRule(request);
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientKey = resolveClientKey(request, rule);
        String bucketKey = rule.key() + ":" + clientKey;
        RateLimitDecision decision = rateLimitService.evaluate(bucketKey, rule.maxRequests(), rule.window());
        if (decision.allowed()) {
            filterChain.doFilter(request, response);
            return;
        }

        log.warn("Rate limit exceeded for rule={} client={}", rule.key(), clientKey);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        if (decision.retryAfterSeconds() > 0) {
            response.setHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()));
        }
        ApiResponse<Void> body = new ApiResponse<>(
                GlobalCode.RATE_LIMIT_EXCEEDED.getCode(),
                GlobalCode.RATE_LIMIT_EXCEEDED.getMessage(),
                null
        );
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private RateLimitRule resolveRule(HttpServletRequest request) {
        String method = request.getMethod().toUpperCase(Locale.ROOT);
        String path = request.getServletPath();

        if ("POST".equals(method)) {
            if ("/api/v1/auth/login".equals(path)) {
                return LOGIN_RULE;
            }
            if ("/api/v1/auth/register".equals(path)) {
                return REGISTER_RULE;
            }
            if ("/api/v1/auth/refresh-token".equals(path)) {
                return REFRESH_TOKEN_RULE;
            }
            if ("/api/v1/discounts/validate".equals(path)) {
                return PUBLIC_SEARCH_RULE;
            }
            if ("/api/v1/admin/products".equals(path) || "/api/v1/admin/products/assign-discount".equals(path)) {
                return SENSITIVE_MUTATE_RULE;
            }
            if ("/api/v1/admin/discounts".equals(path)) {
                return SENSITIVE_MUTATE_RULE;
            }
        }

        if ("GET".equals(method)) {
            if ("/api/v1/products".equals(path) || "/api/v1/products/search".equals(path)) {
                return PUBLIC_SEARCH_RULE;
            }
            if ("/api/v1/payment/create-url".equals(path)) {
                return SENSITIVE_MUTATE_RULE;
            }
        }

        if ("PUT".equals(method) || "DELETE".equals(method)) {
            if (matchesAnyPrefix(path, List.of("/api/v1/admin/products/", "/api/v1/admin/discounts/"))) {
                return SENSITIVE_MUTATE_RULE;
            }
        }

        if ("PATCH".equals(method)) {
            if (path.matches("^/api/v1/admin/orders/\\d+/status$")) {
                return SENSITIVE_MUTATE_RULE;
            }
            if (path.matches("^/api/v1/admin/discounts/\\d+/toggle-status$")) {
                return SENSITIVE_MUTATE_RULE;
            }
        }

        return null;
    }

    private boolean matchesAnyPrefix(String path, List<String> prefixes) {
        return prefixes.stream().anyMatch(path::startsWith);
    }

    private String resolveClientKey(HttpServletRequest request, RateLimitRule rule) {
        if (rule.preferAuthenticatedUser()) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                String username = authentication.getName();
                if (username != null && !username.isBlank() && !"anonymousUser".equalsIgnoreCase(username)) {
                    return "user:" + username;
                }
            }
        }
        return "ip:" + extractClientIp(request);
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}