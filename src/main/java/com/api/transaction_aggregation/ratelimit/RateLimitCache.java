package com.api.transaction_aggregation.ratelimit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Guava-based rate limit cache.
 * Keys expire after the configured window, automatically resetting counters.
 */
@Component
public class RateLimitCache {

    private static final Logger log = LoggerFactory.getLogger(RateLimitCache.class);

    private final Cache<String, AtomicInteger> requestCounts;
    private int loginLimit;
    private int apiLimit;
    private int tokenGenLimit;

    public RateLimitCache(
            @Value("${rate-limit.window-seconds:60}") long windowSeconds,
            @Value("${rate-limit.login.max-requests:5}") int loginLimit,
            @Value("${rate-limit.api.max-requests:60}") int apiLimit,
            @Value("${rate-limit.token-generate.max-requests:3}") int tokenGenLimit) {

        this.requestCounts = CacheBuilder.newBuilder()
                .expireAfterWrite(windowSeconds, TimeUnit.SECONDS)
                .maximumSize(50000)
                .build();

        this.loginLimit = loginLimit;
        this.apiLimit = apiLimit;
        this.tokenGenLimit = tokenGenLimit;

        log.info("Rate limit cache initialized — login: {}/min, api: {}/min, token-gen: {}/min",
                loginLimit, apiLimit, tokenGenLimit);
    }

    /**
     * Check if request is allowed. Returns true if under limit.
     */
    public boolean isAllowed(String key, int maxRequests) {
        try {
            AtomicInteger count = requestCounts.get(key, () -> new AtomicInteger(0));
            return count.incrementAndGet() <= maxRequests;
        } catch (Exception e) {
            // On cache error, allow the request (fail open)
            return true;
        }
    }

    public int getRemaining(String key, int maxRequests) {
        AtomicInteger count = requestCounts.getIfPresent(key);
        if (count == null) return maxRequests;
        return Math.max(0, maxRequests - count.get());
    }

    public int getLoginLimit() { return loginLimit; }
    public int getApiLimit() { return apiLimit; }
    public int getTokenGenLimit() { return tokenGenLimit; }

    // Admin-adjustable setters
    public void setLoginLimit(int limit) { this.loginLimit = limit; }
    public void setApiLimit(int limit) { this.apiLimit = limit; }
    public void setTokenGenLimit(int limit) { this.tokenGenLimit = limit; }
}
