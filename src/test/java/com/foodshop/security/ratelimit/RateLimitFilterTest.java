package com.foodshop.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.foodshop.dto.ApiResponse;
import com.foodshop.exception.GlobalCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitFilterTest {
    private RateLimitFilter rateLimitFilter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        rateLimitFilter = new RateLimitFilter(new RateLimitService(), objectMapper);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturn429WhenLoginLimitExceeded() throws Exception {
        MockHttpServletResponse lastResponse = performRepeatedRequests("POST", "/api/v1/auth/login", 6, null);

        assertEquals(429, lastResponse.getStatus());
        assertRateLimitBody(lastResponse);
    }

    @Test
    void shouldReturn429WhenRefreshTokenLimitExceededPerUser() throws Exception {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "customer01",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
        MockHttpServletResponse lastResponse = performRepeatedRequests("POST", "/api/v1/auth/refresh-token", 11, authentication);

        assertEquals(429, lastResponse.getStatus());
        assertRateLimitBody(lastResponse);
    }

    @Test
    void shouldReturn429WhenPublicProductListLimitExceeded() throws Exception {
        MockHttpServletResponse lastResponse = performRepeatedRequests("GET", "/api/v1/products", 61, null);

        assertEquals(429, lastResponse.getStatus());
        assertRateLimitBody(lastResponse);
    }

    @Test
    void shouldReturn429WhenAdminMutateLimitExceeded() throws Exception {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "admin01",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        MockHttpServletResponse lastResponse = performRepeatedRequests("PATCH", "/api/v1/admin/orders/12/status", 31, authentication);

        assertEquals(429, lastResponse.getStatus());
        assertRateLimitBody(lastResponse);
    }

    private MockHttpServletResponse performRepeatedRequests(String method,
                                                            String path,
                                                            int totalRequests,
                                                            UsernamePasswordAuthenticationToken authentication) throws Exception {
        MockHttpServletResponse lastResponse = null;
        for (int i = 0; i < totalRequests; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest(method, path);
            request.setServletPath(path);
            request.setRemoteAddr("127.0.0.1");
            MockHttpServletResponse response = new MockHttpServletResponse();
            if (authentication != null) {
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                SecurityContextHolder.clearContext();
            }
            rateLimitFilter.doFilter(request, response, new MockFilterChain());
            lastResponse = response;
        }
        return lastResponse;
    }

    private void assertRateLimitBody(MockHttpServletResponse response) throws Exception {
        ApiResponse<?> apiResponse = objectMapper.readValue(response.getContentAsString(), ApiResponse.class);
        assertEquals(GlobalCode.RATE_LIMIT_EXCEEDED.getCode(), apiResponse.getCode());
        assertEquals(GlobalCode.RATE_LIMIT_EXCEEDED.getMessage(), apiResponse.getMessage());
        assertTrue(response.getHeader("Retry-After") != null && !response.getHeader("Retry-After").isBlank());
    }
}
