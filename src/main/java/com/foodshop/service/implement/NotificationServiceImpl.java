package com.foodshop.service.implement;

import com.foodshop.dto.response.NotificationResponse;
import com.foodshop.entity.Notification;
import com.foodshop.entity.Order;
import com.foodshop.entity.User;
import com.foodshop.exception.GlobalCode;
import com.foodshop.exception.GlobalException;
import com.foodshop.repository.NotificationRepository;
import com.foodshop.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private static final String NOTIFICATION_TYPE_ORDER_STATUS = "ORDER_STATUS";

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationEmailService notificationEmailService;
    private final OrderEmailModelFactory orderEmailModelFactory;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByUserId(Integer userId) {
        return notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(Integer userId) {
        return notificationRepository.countByUser_UserIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Integer userId, Integer notificationId) {
        Notification notification = notificationRepository.findByNotificationIdAndUser_UserId(notificationId, userId)
                .orElseThrow(() -> new GlobalException(GlobalCode.NOT_FOUND, "Notification not found."));
        if (!Boolean.TRUE.equals(notification.getIsRead())) {
            notification.setIsRead(true);
            notification = notificationRepository.save(notification);
        }
        return toResponse(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(Integer userId) {
        notificationRepository.markAllAsRead(userId);
    }

    @Override
    @Transactional
    public void notifyOrderStatusChanged(Order order) {
        User user = order.getUser();
        if (user == null || user.getUserId() == null) {
            return;
        }

        Notification notification = new Notification();
        notification.setUser(user);
        notification.setOrder(order);
        notification.setType(NOTIFICATION_TYPE_ORDER_STATUS);
        notification.setTitle("Order #" + order.getOrderId() + " updated");
        notification.setContent("Your order status is now " + order.getStatus() + ".");
        notification.setIsRead(false);

        Notification savedNotification = notificationRepository.save(notification);
        NotificationResponse response = toResponse(savedNotification);
        messagingTemplate.convertAndSendToUser(user.getUsername(), "/queue/notifications", response);

        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            notificationEmailService.sendOrderStatusEmail(orderEmailModelFactory.buildForStatusUpdate(order));
        }
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getNotificationId())
                .orderId(notification.getOrder() != null ? notification.getOrder().getOrderId() : null)
                .type(notification.getType())
                .title(notification.getTitle())
                .content(notification.getContent())
                .read(Boolean.TRUE.equals(notification.getIsRead()))
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
