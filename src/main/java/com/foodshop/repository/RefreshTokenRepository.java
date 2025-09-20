package com.foodshop.repository;

import com.foodshop.entity.RefreshToken;
import com.foodshop.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    int deleteByUser(User user);
    Optional<RefreshToken> findByRefreshToken(String refreshToken);
}
