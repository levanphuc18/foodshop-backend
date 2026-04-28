package com.foodshop.controller;

import com.foodshop.dto.ApiResponse;
import com.foodshop.dto.response.NotificationResponse;
import com.foodshop.exception.GlobalCode;
import com.foodshop.exception.GlobalException;
import com.foodshop.repository.UserRepository;
import com.foodshop.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications(Authentication authentication) {
        Integer userId = resolveUserId(authentication);
        List<NotificationResponse> notifications = notificationService.getNotificationsByUserId(userId);
        return ResponseEntity.ok(new ApiResponse<>(GlobalCode.SUCCESS, "Notifications fetched successfully.", notifications));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(Authentication authentication) {
        Integer userId = resolveUserId(authentication);
        long unreadCount = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(new ApiResponse<>(GlobalCode.SUCCESS, "Unread count fetched successfully.", unreadCount));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(@PathVariable Integer notificationId,
                                                                        Authentication authentication) {
        Integer userId = resolveUserId(authentication);
        NotificationResponse response = notificationService.markAsRead(userId, notificationId);
        return ResponseEntity.ok(new ApiResponse<>(GlobalCode.SUCCESS, "Notification marked as read.", response));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(Authentication authentication) {
        Integer userId = resolveUserId(authentication);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(new ApiResponse<>(GlobalCode.SUCCESS, "All notifications marked as read.", null));
    }

    private Integer resolveUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new GlobalException(GlobalCode.UNAUTHORIZED);
        }
        return userRepository.getUserIdByUsername(authentication.getName());
    }
}
