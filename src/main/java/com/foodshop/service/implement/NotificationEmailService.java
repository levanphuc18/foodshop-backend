package com.foodshop.service.implement;

import com.foodshop.dto.email.OrderEmailModel;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.Year;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEmailService {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.notification.email.mock:true}")
    private boolean mockEmailEnabled;

    @Value("${app.notification.email.from:}")
    private String fromAddress;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Async
    public void sendOrderCreatedEmail(OrderEmailModel orderEmail) {
        sendOrderEmail(
                orderEmail.getCustomerEmail(),
                "Foodshop order received #" + orderEmail.getOrderId(),
                "mail/order-created",
                orderEmail
        );
    }

    @Async
    public void sendOrderStatusEmail(OrderEmailModel orderEmail) {
        sendOrderEmail(
                orderEmail.getCustomerEmail(),
                "Foodshop order update #" + orderEmail.getOrderId(),
                "mail/order-status-updated",
                orderEmail
        );
    }

    private void sendOrderEmail(String recipientEmail, String subject, String templateName, OrderEmailModel orderEmail) {
        if (recipientEmail == null || recipientEmail.isBlank()) {
            return;
        }

        String html = renderHtml(templateName, orderEmail);

        if (mockEmailEnabled) {
            log.info("Mock email sent to={} subject={} template={} html={}",
                    recipientEmail,
                    subject,
                    templateName,
                    html.replace('\n', ' ').replaceAll("\\s{2,}", " "));
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            String effectiveFrom = fromAddress != null && !fromAddress.isBlank() ? fromAddress : mailUsername;
            if (effectiveFrom != null && !effectiveFrom.isBlank()) {
                helper.setFrom(effectiveFrom);
            }

            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);

            log.info("Notification email sent to={} subject={} orderId={}",
                    recipientEmail,
                    subject,
                    orderEmail.getOrderId());
        } catch (Exception ex) {
            log.error("Failed to send notification email to={} subject={} orderId={}",
                    recipientEmail,
                    subject,
                    orderEmail.getOrderId(),
                    ex);
        }
    }

    private String renderHtml(String templateName, OrderEmailModel orderEmail) {
        Context context = new Context(Locale.ENGLISH);
        context.setVariable("customerName", orderEmail.getCustomerName());
        context.setVariable("orderId", orderEmail.getOrderId());
        context.setVariable("status", orderEmail.getStatus());
        context.setVariable("statusLabel", orderEmail.getStatusLabel());
        context.setVariable("statusMessage", orderEmail.getStatusMessage());
        context.setVariable("createdAtFormatted", orderEmail.getCreatedAtFormatted());
        context.setVariable("shippingAddress", orderEmail.getShippingAddress());
        context.setVariable("shippingNote", orderEmail.getShippingNote());
        context.setVariable("discountCode", orderEmail.getDiscountCode());
        context.setVariable("orderUrl", orderEmail.getOrderUrl());
        context.setVariable("totalAmount", orderEmail.getTotalAmount());
        context.setVariable("discountAmount", orderEmail.getDiscountAmount());
        context.setVariable("shippingFee", orderEmail.getShippingFee());
        context.setVariable("shippingDiscount", orderEmail.getShippingDiscount());
        context.setVariable("finalAmount", orderEmail.getFinalAmount());
        context.setVariable("items", orderEmail.getItems());
        context.setVariable("currentYear", Year.now().getValue());
        return templateEngine.process(templateName, context);
    }
}
