package com.foodshop.service.implement;

import com.foodshop.dto.request.RegisterRequest;
import com.foodshop.dto.response.AuthResponse;
import com.foodshop.dto.response.JwtResponse;
import com.foodshop.dto.response.RefreshTokenResponse;
import com.foodshop.entity.RefreshToken;
import com.foodshop.entity.User;
import com.foodshop.enums.Role;
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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import org.springframework.transaction.annotation.Transactional;

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

    private String hashToken(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing token", e);
        }
    }

    protected AuthResponse convertToRegisterResponse(User user, String accessToken, String refreshToken) {
        return new AuthResponse(user.getUsername(), accessToken, refreshToken, user.getUserId(), user.getRole());
    }

    @Override
    @Transactional
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
    @Transactional
    public RefreshTokenResponse generateRefreshToken(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(GlobalCode.USER_NOT_FOUND));

        // SECURITY FIX: Revoke old tokens before issuing a new one
        refreshTokenRepository.deleteByUser_UserId(userId);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        
        String rawToken = UUID.randomUUID().toString();
        // SECURITY FIX: Hash token trước khi lưu vào DB
        refreshToken.setRefreshToken(hashToken(rawToken));
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshExpirationMs));
        
        refreshTokenRepository.save(refreshToken);
        
        // Trả về raw token cho client (DB chỉ lưu bản hash)
        return new RefreshTokenResponse(rawToken, refreshToken.getExpiryDate());
    }

    @Override
    public String validateRefreshTokenAndGetUsername(String refreshToken) {
        // SECURITY FIX: So sánh token bằng bản hash
        String hashedToken = hashToken(refreshToken);
        RefreshToken token = refreshTokenRepository.findByRefreshToken(hashedToken)
                .orElse(null);

        if (token == null || token.getExpiryDate().isBefore(Instant.now()) || !token.getUser().getEnabled()) {
            throw new GlobalException(GlobalCode.INVALID_REFRESH_TOKEN);
        }

        return token.getUser().getUsername();
    }

    @Override
    public String createAccessTokenFromRefreshToken(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new GlobalException(GlobalCode.USER_NOT_FOUND));
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", user.getRole().name());
        return jwtUtil.generateAccessToken(username, claims);
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest registerRequest) {
        boolean checkUsername = userRepository.existsByUsername(registerRequest.getUsername());
        if (checkUsername) {
            throw new GlobalException(GlobalCode.USERNAME_EXISTS);
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
            throw new GlobalException(GlobalCode.USER_NOT_FOUND);
        }
        return userId;
    }

    @Override
    public String getRoleByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(user -> user.getRole().name())
                .orElseThrow(() -> new GlobalException(GlobalCode.USER_NOT_FOUND));
    }
}
