package com.foodshop.mapper;

import com.foodshop.entity.User;
import com.foodshop.dto.response.UserResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toUserResponse(User user);
}
