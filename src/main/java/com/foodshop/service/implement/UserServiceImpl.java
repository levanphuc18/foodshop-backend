package com.foodshop.service.implement;

import com.foodshop.dto.response.UserResponse;
import com.foodshop.entity.User;
import com.foodshop.enums.Role;
import com.foodshop.exception.GlobalCode;
import com.foodshop.exception.GlobalException;
import com.foodshop.mapper.UserMapper;
import com.foodshop.repository.UserRepository;
import com.foodshop.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final com.foodshop.repository.RefreshTokenRepository refreshTokenRepository;

    @Override
    public Page<UserResponse> getAllUsersAdmin(String keyword, Role role, Boolean enabled, int page, int size, String sortBy, boolean asc) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        Specification<User> specification = (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();

            if (!normalizedKeyword.isBlank()) {
                String pattern = "%" + normalizedKeyword + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern),
                        cb.like(cb.lower(root.get("username")), pattern),
                        cb.like(cb.lower(root.get("phoneNumber")), pattern)
                ));
            }

            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }

            if (enabled != null) {
                predicates.add(cb.equal(root.get("enabled"), enabled));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Pageable pageable = PageRequest.of(page, size, resolveUserSort(sortBy, asc));
        return userRepository.findAll(specification, pageable)
                .map(userMapper::toUserResponse);
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

    private Sort resolveUserSort(String sortBy, boolean asc) {
        String property = switch (sortBy == null ? "" : sortBy) {
            case "fullName" -> "fullName";
            case "username" -> "username";
            default -> "createdAt";
        };

        return asc ? Sort.by(property).ascending() : Sort.by(property).descending();
    }
}
