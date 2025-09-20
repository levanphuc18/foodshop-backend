package com.foodshop.service.implement;

import com.foodshop.dto.request.RegisterRequest;
import com.foodshop.dto.response.AuthResponse;
import com.foodshop.dto.response.JwtResponse;
import com.foodshop.dto.response.RefreshTokenResponse;
import com.foodshop.entity.RefreshToken;
import com.foodshop.entity.User;
import com.foodshop.enums.Role;
import com.foodshop.exception.EntityExitsException;
import com.foodshop.exception.GlobalCode;
import com.foodshop.exception.GlobalException;
import com.foodshop.repository.RefreshTokenRepository;
import com.foodshop.repository.UserRepository;
import com.foodshop.security.JwtUtil;
import com.foodshop.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    @Autowired
    private PasswordEncoder encoder;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt_refresh_expiration}")
    private long refreshExpirationMs;

    protected AuthResponse convertToRegisterResponse(User user, String accessToken, String refreshToken) {
        return new AuthResponse(user.getUsername(), accessToken, refreshToken, user.getUserId());
    }

    @Override
    public JwtResponse generateToken(String username, String password) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );

        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", auth.getAuthorities());

        String accessToken = jwtUtil.generateAccessToken(username, claims);

        Integer userId = userRepository.getUserIdByUsername(username);
        RefreshTokenResponse refreshTokenResponse = generateRefreshToken(userId);

        return new JwtResponse(accessToken, refreshTokenResponse.getRefreshToken());
    }

    @Override
    public RefreshTokenResponse generateRefreshToken(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setRefreshToken(UUID.randomUUID().toString());
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshExpirationMs));
        refreshTokenRepository.save(refreshToken);
        return new RefreshTokenResponse(refreshToken.getRefreshToken(), refreshToken.getExpiryDate());
    }

    @Override
    public String validateRefreshTokenAndGetUsername(String refreshToken) {
        RefreshToken token = refreshTokenRepository.findByRefreshToken(refreshToken)
                .orElse(null);

        if (token == null || token.getExpiryDate().isBefore(Instant.now())) {
            return null;
        }

        return token.getUser().getUsername();
    }

    @Override
    public String createAccessTokenFromRefreshToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", "CUSTOMER");
        return jwtUtil.generateAccessToken(username, claims);
    }

    @Override
    public AuthResponse register(RegisterRequest registerRequest) {
        boolean checkUsername = userRepository.existsByUsername(registerRequest.getUsername());
        if (checkUsername) {
            throw new GlobalException(GlobalCode.USER_ALREADY_EXISTS);
        }
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(encoder.encode(registerRequest.getPassword()));
        user.setEmail(registerRequest.getEmail());
        user.setAddress(registerRequest.getAddress());
        user.setPhoneNumber(registerRequest.getPhoneNumber());
        user.setFullName(registerRequest.getFullName());
        user.setRole(Role.CUSTOMER);

        user = userRepository.save(user);

        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", user.getRole().name());
        String accessToken = jwtUtil.generateAccessToken(user.getUsername(), claims);

        RefreshTokenResponse refreshTokenResponse = this.generateRefreshToken(user.getUserId());

        return convertToRegisterResponse(user, accessToken, refreshTokenResponse.getRefreshToken());
    }

    @Override
    public Integer getUserIdByUsername(String username) {
        Integer userId = userRepository.getUserIdByUsername(username);
        if (userId == null) {
            throw new UsernameNotFoundException("Khong tim thay username: " + username);
        }
        return userId;
    }
}