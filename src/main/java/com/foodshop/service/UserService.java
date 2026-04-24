package com.foodshop.service;

import com.foodshop.dto.response.UserResponse;
import java.util.List;

public interface UserService {
    List<UserResponse> getAllUsers();
    UserResponse getUserById(Integer id);
    UserResponse toggleUserStatus(Integer id);
}
