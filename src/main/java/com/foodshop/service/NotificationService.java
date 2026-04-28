package com.foodshop.service;

import com.foodshop.dto.response.NotificationResponse;
import com.foodshop.entity.Order;

import java.util.List;

public interface NotificationService {
    List<NotificationResponse> getNotificationsByUserId(Integer userId);

    long getUnreadCount(Integer userId);

    NotificationResponse markAsRead(Integer userId, Integer notificationId);

    void markAllAsRead(Integer userId);

    void notifyOrderStatusChanged(Order order);
}
