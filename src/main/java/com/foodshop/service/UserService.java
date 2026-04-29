package com.foodshop.service;

import com.foodshop.dto.response.UserResponse;
import com.foodshop.enums.Role;
import org.springframework.data.domain.Page;

import java.util.List;

public interface UserService {
    Page<UserResponse> getAllUsersAdmin(String keyword, Role role, Boolean enabled, int page, int size, String sortBy, boolean asc);

    UserResponse getUserById(Integer id);

    UserResponse toggleUserStatus(Integer id);
}
