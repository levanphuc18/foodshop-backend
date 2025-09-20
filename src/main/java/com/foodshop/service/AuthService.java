package com.foodshop.service;

import com.foodshop.dto.request.RegisterRequest;
import com.foodshop.dto.response.AuthResponse;
import com.foodshop.dto.response.JwtResponse;
import com.foodshop.dto.response.RefreshTokenResponse;
import org.springframework.stereotype.Service;

@Service
public interface AuthService {
    JwtResponse generateToken(String username, String password);
    RefreshTokenResponse generateRefreshToken(Integer userId);
    String validateRefreshTokenAndGetUsername(String refreshToken);
    String createAccessTokenFromRefreshToken(String username);
    AuthResponse register(RegisterRequest registerRequest);
    Integer getUserIdByUsername(String username);
}

