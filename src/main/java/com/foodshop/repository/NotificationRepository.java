package com.foodshop.repository;

import com.foodshop.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    List<Notification> findByUser_UserIdOrderByCreatedAtDesc(Integer userId);

    long countByUser_UserIdAndIsReadFalse(Integer userId);

    Optional<Notification> findByNotificationIdAndUser_UserId(Integer notificationId, Integer userId);

    @Modifying
    @Query("""
        update Notification n
        set n.isRead = true
        where n.user.userId = :userId and n.isRead = false
    """)
    int markAllAsRead(@Param("userId") Integer userId);
}
