package com.foodshop.controller;

import com.foodshop.dto.request.AuthRequest;
import com.foodshop.dto.request.RefreshTokenRequest;
import com.foodshop.dto.request.RegisterRequest;
import com.foodshop.dto.response.AuthResponse;
import com.foodshop.dto.ApiResponse;
import com.foodshop.dto.response.JwtResponse;
import com.foodshop.exception.GlobalCode;
import com.foodshop.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
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
        System.out.println("Registering user: " + authResponse.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody AuthRequest authRequest) {
        JwtResponse jwtResponse = authService.generateToken(authRequest.getUsername(), authRequest.getPassword());
        Integer userId = authService.getUserIdByUsername(authRequest.getUsername());

        AuthResponse authResponse = new AuthResponse(
                authRequest.getUsername(),
                jwtResponse.getAccessToken(),
                jwtResponse.getRefreshToken(),
                userId
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

        try {
            String username = authService.validateRefreshTokenAndGetUsername(refreshToken);

            if (username == null) {
                ApiResponse<JwtResponse> errorResponse = new ApiResponse<>(
                        GlobalCode.BAD_REQUEST,
                        "Invalid or expired refresh token.",
                        null
                );
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            String newAccessToken = authService.createAccessTokenFromRefreshToken(username);

            JwtResponse jwtResponse = new JwtResponse(newAccessToken, refreshToken);

            ApiResponse<JwtResponse> response = new ApiResponse<>(
                    GlobalCode.SUCCESS,
                    "Refresh token successful.",
                    jwtResponse
            );

            return ResponseEntity.status(HttpStatus.OK).body(response);

        } catch (Exception e) {
            ApiResponse<JwtResponse> errorResponse = new ApiResponse<>(
                    GlobalCode.TOKEN_EXPIRED,
                    "Error refreshing token: " + e.getMessage(),
                    null
            );
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}