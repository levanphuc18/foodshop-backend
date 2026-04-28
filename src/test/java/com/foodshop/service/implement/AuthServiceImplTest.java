package com.foodshop.service.implement;

import com.foodshop.dto.request.RegisterRequest;
import com.foodshop.dto.response.AuthResponse;
import com.foodshop.entity.RefreshToken;
import com.foodshop.entity.User;
import com.foodshop.enums.Role;
import com.foodshop.exception.GlobalCode;
import com.foodshop.exception.GlobalException;
import com.foodshop.repository.RefreshTokenRepository;
import com.foodshop.repository.UserRepository;
import com.foodshop.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "encoder", passwordEncoder);
        ReflectionTestUtils.setField(authService, "refreshExpirationMs", 60000L);
    }

    @Test
    void validateRefreshTokenAndGetUsernameShouldRejectExpiredToken() {
        User user = buildUser(10, "customer01", true);
        RefreshToken token = new RefreshToken();
        token.setRefreshToken("expired-token");
        token.setUser(user);
        token.setExpiryDate(Instant.now().minusSeconds(30));
        when(refreshTokenRepository.findByRefreshToken("expired-token")).thenReturn(Optional.of(token));

        GlobalException exception = assertThrows(GlobalException.class,
                () -> authService.validateRefreshTokenAndGetUsername("expired-token"));

        assertEquals(GlobalCode.INVALID_REFRESH_TOKEN.getCode(), exception.getCode());
    }

    @Test
    void validateRefreshTokenAndGetUsernameShouldReturnUsernameWhenValid() {
        User user = buildUser(10, "customer01", true);
        RefreshToken token = new RefreshToken();
        token.setRefreshToken("valid-token");
        token.setUser(user);
        token.setExpiryDate(Instant.now().plusSeconds(300));
        when(refreshTokenRepository.findByRefreshToken("valid-token")).thenReturn(Optional.of(token));

        String username = authService.validateRefreshTokenAndGetUsername("valid-token");

        assertEquals("customer01", username);
    }

    @Test
    void registerShouldPersistCustomerAndReturnTokens() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("Password1");
        request.setEmail("newuser@example.com");
        request.setFullName("New User");
        request.setPhoneNumber("0987654321");
        request.setAddress("123 Main St");

        User savedUser = buildUser(99, "newuser", true);
        savedUser.setEmail("newuser@example.com");
        savedUser.setFullName("New User");
        savedUser.setPhoneNumber("0987654321");
        savedUser.setAddress("123 Main St");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("Password1")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userRepository.findById(99)).thenReturn(Optional.of(savedUser));
        when(jwtUtil.generateAccessToken(eq("newuser"), any(Map.class))).thenReturn("access-token");

        AuthResponse response = authService.register(request);

        assertEquals("newuser", response.getUsername());
        assertEquals(Role.CUSTOMER, response.getRole());
        assertEquals("access-token", response.getAccessToken());
        assertNotNull(response.getRefreshToken());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("encoded-password", userCaptor.getValue().getPassword());
        assertEquals(Role.CUSTOMER, userCaptor.getValue().getRole());

        ArgumentCaptor<RefreshToken> refreshTokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
        assertEquals(savedUser, refreshTokenCaptor.getValue().getUser());
    }

    @Test
    void createAccessTokenFromRefreshTokenShouldUseUserRoleClaim() {
        User user = buildUser(10, "customer01", true);
        when(userRepository.findByUsername("customer01")).thenReturn(Optional.of(user));
        when(jwtUtil.generateAccessToken(eq("customer01"), any(Map.class))).thenReturn("new-access-token");

        String token = authService.createAccessTokenFromRefreshToken("customer01");

        assertEquals("new-access-token", token);
    }

    private User buildUser(int userId, String username, boolean enabled) {
        User user = new User();
        user.setUserId(userId);
        user.setUsername(username);
        user.setPassword("encoded-password");
        user.setRole(Role.CUSTOMER);
        user.setEnabled(enabled);
        return user;
    }
}
