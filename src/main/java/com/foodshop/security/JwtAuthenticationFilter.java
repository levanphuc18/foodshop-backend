package com.foodshop.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getServletPath();
        String method = request.getMethod();

        // Skip JWT check for public endpoints
        boolean isPublicAuth = path.startsWith("/api/v1/auth") || path.startsWith("/api/v1/users");
        boolean isPublicGet = "GET".equalsIgnoreCase(method) && (
                path.startsWith("/api/v1/products") ||
                path.startsWith("/api/v1/categories") ||
                path.startsWith("/api/v1/discounts")
        );

        if (isPublicAuth || isPublicGet) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");
        final String tokenPrefix = "Bearer ";
        String username = null;
        String jwt = null;

        if (authHeader != null && authHeader.startsWith(tokenPrefix)) {
            jwt = authHeader.substring(tokenPrefix.length());
        } else if (request.getCookies() != null) {
            // SECURITY FIX [P0]: Hỗ trợ đọc HttpOnly cookie từ Frontend gửi lên
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if ("auth-token".equals(cookie.getName())) {
                    jwt = cookie.getValue();
                    break;
                }
            }
        }

        if (jwt != null) {
            try {
                username = jwtUtil.extractUsername(jwt);
            } catch (Exception ignored) {
            }
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (jwtUtil.isTokenValid(jwt, username) && userDetails.isEnabled()) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        filterChain.doFilter(request, response);
    }
}
