package com.foodshop.controller;

import com.foodshop.dto.ApiResponse;
import com.foodshop.dto.request.UpdateProfileRequest;
import com.foodshop.dto.response.UserResponse;
import com.foodshop.exception.GlobalCode;
import com.foodshop.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserProfileController {
    private final UserService userService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile(Authentication authentication) {
        UserResponse data = userService.getCurrentUserProfile(authentication.getName());
        return ResponseEntity.ok(new ApiResponse<>(GlobalCode.SUCCESS, "Profile fetched successfully.", data));
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UserResponse data = userService.updateCurrentUserProfile(authentication.getName(), request);
        return ResponseEntity.ok(new ApiResponse<>(GlobalCode.SUCCESS, "Profile updated successfully.", data));
    }
}

