package com.foodshop.controller;

import com.foodshop.dto.ApiResponse;
import com.foodshop.dto.response.PageResponse;
import com.foodshop.dto.response.UserResponse;
import com.foodshop.enums.Role;
import com.foodshop.exception.GlobalCode;
import com.foodshop.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> getAllUsers(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean enabled,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "false") boolean asc) {
        PageResponse<UserResponse> users = PageResponse.from(
                userService.getAllUsersAdmin(keyword, role, enabled, page, size, sortBy, asc)
        );
        return ResponseEntity.ok(new ApiResponse<>(GlobalCode.SUCCESS, users));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Integer id) {
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(new ApiResponse<>(GlobalCode.SUCCESS, user));
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<ApiResponse<UserResponse>> toggleUserStatus(@PathVariable Integer id) {
        UserResponse user = userService.toggleUserStatus(id);
        String message = user.getEnabled() ? "User account enabled." : "User account disabled.";
        return ResponseEntity.ok(new ApiResponse<>(GlobalCode.SUCCESS, message, user));
    }
}
