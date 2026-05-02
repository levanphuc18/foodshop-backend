package com.foodshop.security;

import com.foodshop.dto.response.AuthResponse;
import com.foodshop.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final ObjectProvider<AuthService> authServiceProvider;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        if (email == null || email.isBlank()) {
            response.sendRedirect(frontendUrl + "/login?oauth=failed");
            return;
        }

        AuthResponse authResponse = authServiceProvider.getObject().loginWithGoogle(email, name);

        ResponseCookie accessCookie = ResponseCookie.from("auth-token", authResponse.getAccessToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(24 * 60 * 60)
                .build();
        response.addHeader("Set-Cookie", accessCookie.toString());

        ResponseCookie refreshCookie = ResponseCookie.from("refresh-token", authResponse.getRefreshToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .build();
        response.addHeader("Set-Cookie", refreshCookie.toString());

        response.sendRedirect(frontendUrl + "/oauth/callback");
    }
}
