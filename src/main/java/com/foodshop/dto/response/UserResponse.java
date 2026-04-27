package com.foodshop.dto.response;

import com.foodshop.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponse {
    private Integer userId;
    private String username;
    private String email;
    private String fullName;
    private String phoneNumber;
    private String address;
    private Role role;
    private Boolean enabled;
    private LocalDateTime createdAt;
}
