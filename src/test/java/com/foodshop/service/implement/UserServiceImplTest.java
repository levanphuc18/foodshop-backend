package com.foodshop.service.implement;

import com.foodshop.dto.request.UpdateProfileRequest;
import com.foodshop.dto.response.UserResponse;
import com.foodshop.entity.User;
import com.foodshop.enums.Role;
import com.foodshop.exception.GlobalException;
import com.foodshop.mapper.UserMapper;
import com.foodshop.repository.RefreshTokenRepository;
import com.foodshop.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void getCurrentUserProfileShouldReturnUserData() {
        User user = buildUser();
        UserResponse mapped = buildResponse();
        when(userRepository.findByUsername("customer01")).thenReturn(Optional.of(user));
        when(userMapper.toUserResponse(user)).thenReturn(mapped);

        UserResponse result = userService.getCurrentUserProfile("customer01");

        assertEquals("customer01", result.getUsername());
        assertEquals("customer01@example.com", result.getEmail());
    }

    @Test
    void updateCurrentUserProfileShouldUpdateFields() {
        User user = buildUser();
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setEmail("new-email@example.com");
        request.setFullName("Customer Updated");
        request.setPhoneNumber("0981111222");
        request.setAddress("Updated Address");

        UserResponse mapped = UserResponse.builder()
                .userId(1)
                .username("customer01")
                .email("new-email@example.com")
                .fullName("Customer Updated")
                .phoneNumber("0981111222")
                .address("Updated Address")
                .role(Role.CUSTOMER)
                .enabled(true)
                .build();

        when(userRepository.findByUsername("customer01")).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("new-email@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userMapper.toUserResponse(any(User.class))).thenReturn(mapped);

        UserResponse result = userService.updateCurrentUserProfile("customer01", request);

        assertEquals("new-email@example.com", result.getEmail());
        assertEquals("Customer Updated", result.getFullName());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateCurrentUserProfileShouldRejectExistingEmail() {
        User user = buildUser();
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setEmail("taken@example.com");
        request.setFullName("Customer One");
        request.setPhoneNumber("0980000111");
        request.setAddress("Address");

        when(userRepository.findByUsername("customer01")).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThrows(GlobalException.class, () -> userService.updateCurrentUserProfile("customer01", request));
    }

    private User buildUser() {
        User user = new User();
        user.setUserId(1);
        user.setUsername("customer01");
        user.setEmail("customer01@example.com");
        user.setFullName("Customer One");
        user.setPhoneNumber("0911222333");
        user.setAddress("Address");
        user.setRole(Role.CUSTOMER);
        user.setEnabled(true);
        return user;
    }

    private UserResponse buildResponse() {
        return UserResponse.builder()
                .userId(1)
                .username("customer01")
                .email("customer01@example.com")
                .fullName("Customer One")
                .phoneNumber("0911222333")
                .address("Address")
                .role(Role.CUSTOMER)
                .enabled(true)
                .build();
    }
}

