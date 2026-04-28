package com.foodshop.security.ratelimit;

public record RateLimitDecision(
        boolean allowed,
        long retryAfterSeconds
) {
}