package com.foodshop.security.ratelimit;

import java.time.Duration;

public record RateLimitRule(
        String key,
        int maxRequests,
        Duration window,
        boolean preferAuthenticatedUser
) {
}