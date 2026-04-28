package com.foodshop.security.ratelimit;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class RateLimitService {
    private static final long CLEANUP_INTERVAL = 500;

    private final Map<String, FixedWindowCounter> counters = new ConcurrentHashMap<>();
    private final AtomicLong requestCount = new AtomicLong(0);
    private final Clock clock;

    public RateLimitService() {
        this(Clock.systemUTC());
    }

    RateLimitService(Clock clock) {
        this.clock = clock;
    }

    public RateLimitDecision evaluate(String bucketKey, int maxRequests, Duration window) {
        long now = clock.millis();
        long windowMillis = window.toMillis();
        AtomicLong retryAfterMillis = new AtomicLong(0);
        AtomicLong allowedFlag = new AtomicLong(1);

        counters.compute(bucketKey, (key, current) -> {
            if (current == null || now - current.windowStartedAt >= windowMillis) {
                return new FixedWindowCounter(now, 1);
            }
            if (current.requestCount >= maxRequests) {
                allowedFlag.set(0);
                retryAfterMillis.set(Math.max(0, windowMillis - (now - current.windowStartedAt)));
                return current;
            }
            current.requestCount++;
            return current;
        });

        cleanupIfNeeded(now, windowMillis);
        return new RateLimitDecision(allowedFlag.get() == 1, secondsCeil(retryAfterMillis.get()));
    }

    private void cleanupIfNeeded(long now, long currentWindowMillis) {
        if (requestCount.incrementAndGet() % CLEANUP_INTERVAL != 0) {
            return;
        }
        counters.entrySet().removeIf(entry -> now - entry.getValue().windowStartedAt >= currentWindowMillis * 2);
    }

    private long secondsCeil(long millis) {
        if (millis <= 0) {
            return 0;
        }
        return (millis + 999) / 1000;
    }

    private static final class FixedWindowCounter {
        private final long windowStartedAt;
        private int requestCount;

        private FixedWindowCounter(long windowStartedAt, int requestCount) {
            this.windowStartedAt = windowStartedAt;
            this.requestCount = requestCount;
        }
    }
}