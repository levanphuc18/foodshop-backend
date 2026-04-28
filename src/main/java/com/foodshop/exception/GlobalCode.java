package com.foodshop.exception;

import lombok.Getter;

@Getter
public enum GlobalCode {
    // Success
    SUCCESS(0, "Success"),

    // System errors
    INTERNAL_SERVER_ERROR(500, "Internal server error, please try again later."),
    BAD_REQUEST(400, "Invalid request."),
    NOT_FOUND(404, "Resource not found."),
    UNAUTHORIZED(401, "You are not logged in or do not have access."),
    FORBIDDEN(403, "You do not have permission to perform this action."),

    // CLOUDINARY
    UPLOAD_FAIL(3000, "Image upload failed."),

    // USER
    USERNAME_EXISTS(1001, "Username already exists."),
    EMAIL_EXISTS(1003, "Email already exists."),
    USER_NOT_FOUND(3002, "User not found."),
    ACCOUNT_DISABLED(3003, "Your account is disabled. Please contact admin."),
    INVALID_REFRESH_TOKEN(4011, "Invalid or expired refresh token."),
    INVALID_JWT_TOKEN(4012, "Invalid or tampered token."),
    RATE_LIMIT_EXCEEDED(4291, "Too many requests. Please try again later."),
    INVALID_CURRENT_PASSWORD(4001, "Current password is incorrect."),
    PASSWORD_MISMATCH(4002, "New password and confirm password do not match."),

    // CATEGORY
    CATEGORY_NAME_EXISTS(2001, "Category name already exists."),
    CATEGORY_NOT_FOUND(2002, "Category not found."),

    // PRODUCT
    PRODUCT_NAME_EXISTS(2001, "Product name already exists."),
    PRODUCT_NOT_FOUND(2002, "Product not found."),
    INSUFFICIENT_STOCK(2003, "Not enough stock available."),
    PRODUCT_IN_USE(2005, "Cannot delete product because it is in use by orders or carts."),

    // CART
    CART_NOT_FOUND(2002, "Cart not found."),
    CART_NOT_ITEM(2002, "No item found in the cart."),
    CART_NO_MATCHING_ITEMS(2003, "No matching items found in the cart to delete."),

    // ORDER
    ORDER_NOT_FOUND(2004, "Order not found."),
    ORDER_STATUS_INVALID_FOR_PAYMENT(6003, "Only orders in PENDING status can be paid."),

    // DISCOUNT
    DISCOUNT_CODE_EXISTS(3001, "Discount code already exists."),
    DISCOUNT_NOT_FOUND(3002, "Discount not found."),
    DISCOUNT_INVALID_DATE_RANGE(3003, "End date must be greater than start date."),
    DISCOUNT_NOT_VALID(3004, "Discount code is not valid or has expired."),
    DISCOUNT_NOT_APPLICABLE(3005, "The order does not meet the minimum spend for this discount."),
    MIN_ORDER_AMOUNT_REQUIRED(5000, "MinOrderAmount is required for order-based discounts."),
    INVALID_MIN_ORDER_AMOUNT(5001, "MinOrderAmount must not be provided for product-based discounts."),
    INVALID_MAX_DISCOUNT(5002, "MaxDiscount must not be provided for product-based discounts."),
    INVALID_PERCENTAGE_VALUE(5003, "Percentage value must be between 0 and 100."),

    // PAYMENT
    PAYMENT_NOT_FOUND(6001, "Payment not found."),
    PAYMENT_FAILED(6002, "Payment failed."),

    // REVIEW
    REVIEW_NOT_FOUND(7001, "Review not found."),
    ALREADY_REVIEWED(7002, "You have already reviewed this product."),

    // MESSAGE
    MESSAGE_NOT_FOUND(8001, "Message not found.");

    private final int code;
    private final String message;

    GlobalCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
