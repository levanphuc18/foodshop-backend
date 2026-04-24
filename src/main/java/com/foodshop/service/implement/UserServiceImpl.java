package com.foodshop.service.implement;

import com.foodshop.dto.response.UserResponse;
import com.foodshop.entity.User;
import com.foodshop.exception.GlobalCode;
import com.foodshop.exception.GlobalException;
import com.foodshop.mapper.UserMapper;
import com.foodshop.repository.UserRepository;
import com.foodshop.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final com.foodshop.repository.RefreshTokenRepository refreshTokenRepository;

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toUserResponse)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponse getUserById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new GlobalException(GlobalCode.USER_NOT_FOUND));
        return userMapper.toUserResponse(user);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public UserResponse toggleUserStatus(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new GlobalException(GlobalCode.USER_NOT_FOUND));
        
        if (user.getRole() == com.foodshop.enums.Role.ADMIN) {
            throw new GlobalException(GlobalCode.FORBIDDEN);
        }
        user.setEnabled(!user.getEnabled());
        user = userRepository.save(user);

        // Nếu bị khóa, xóa hết refresh token để buộc logout
        if (!user.getEnabled()) {
            refreshTokenRepository.deleteByUser_UserId(id);
        }
        
        return userMapper.toUserResponse(user);
    }
}
