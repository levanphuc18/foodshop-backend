package com.foodshop.controller;

import com.foodshop.dto.request.AuthRequest;
import com.foodshop.dto.request.RefreshTokenRequest;
import com.foodshop.dto.request.RegisterRequest;
import com.foodshop.entity.User;
import com.foodshop.dto.response.AuthResponse;
import com.foodshop.dto.ApiResponse;
import com.foodshop.dto.response.JwtResponse;
import com.foodshop.enums.Role;
import com.foodshop.exception.GlobalCode;
import com.foodshop.service.AuthService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    @GetMapping("/ping")
    public String ping() {
        return "Server is running!";
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        AuthResponse authResponse = authService.register(registerRequest);
        ApiResponse<AuthResponse> apiResponse = new ApiResponse<>(
                GlobalCode.SUCCESS,
                "Account created successfully.",
                authResponse
        );
        log.info("Registered new user with username={}", authResponse.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest authRequest) {
        JwtResponse jwtResponse = authService.generateToken(authRequest.getUsername(), authRequest.getPassword());
        Integer userId = authService.getUserIdByUsername(authRequest.getUsername());
        String role = authService.getRoleByUsername(authRequest.getUsername());

        AuthResponse authResponse = new AuthResponse(
                authRequest.getUsername(),
                jwtResponse.getAccessToken(),
                jwtResponse.getRefreshToken(),
                userId,
                Role.valueOf(role)
        );
        ApiResponse<AuthResponse> response = new ApiResponse<>(
                GlobalCode.SUCCESS,
                "Login successful.",
                authResponse
        );
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<JwtResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        String username = authService.validateRefreshTokenAndGetUsername(refreshToken);
        String newAccessToken = authService.createAccessTokenFromRefreshToken(username);

        JwtResponse jwtResponse = new JwtResponse(newAccessToken, refreshToken);

        ApiResponse<JwtResponse> response = new ApiResponse<>(
                GlobalCode.SUCCESS,
                "Refresh token successful.",
                jwtResponse
        );

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AuthResponse>> me(Authentication authentication) {
        User user = authService.getUserByUsername(authentication.getName());
        AuthResponse authResponse = new AuthResponse(
                user.getUsername(),
                "",
                "",
                user.getUserId(),
                user.getRole()
        );
        return ResponseEntity.ok(new ApiResponse<>(GlobalCode.SUCCESS, "Current user fetched.", authResponse));
    }
}
