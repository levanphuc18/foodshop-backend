package com.foodshop.security;

import com.foodshop.dto.response.AuthResponse;
import com.foodshop.enums.Role;
import com.foodshop.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    @Mock
    private AuthService authService;
    @Mock
    private ObjectProvider<AuthService> authServiceProvider;

    @Test
    void onAuthenticationSuccessShouldSetCookiesAndRedirect() throws Exception {
        OAuth2AuthenticationSuccessHandler handler = new OAuth2AuthenticationSuccessHandler(authServiceProvider);
        ReflectionTestUtils.setField(handler, "frontendUrl", "http://localhost:3000");

        OAuth2User oauthUser = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("email", "oauth@example.com", "name", "OAuth User"),
                "email"
        );

        AuthResponse authResponse = new AuthResponse(
                "oauthuser",
                "access-token",
                "refresh-token",
                1,
                Role.CUSTOMER
        );
        when(authServiceProvider.getObject()).thenReturn(authService);
        when(authService.loginWithGoogle("oauth@example.com", "OAuth User")).thenReturn(authResponse);

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(oauthUser, null);

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(authServiceProvider).getObject();
        verify(authService).loginWithGoogle("oauth@example.com", "OAuth User");
        assertEquals("http://localhost:3000/oauth/callback", response.getRedirectedUrl());
        assertTrue(response.getHeaders("Set-Cookie").size() >= 2);
    }
}
