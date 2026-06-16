package com.api.transaction_aggregation.ratelimit;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Rate limiting filter. Runs after JwtAuthFilter (order 2).
 * Limits requests per user (authenticated) or per IP (login).
 */
public class RateLimitFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final RateLimitCache rateLimitCache;

    public RateLimitFilter(RateLimitCache rateLimitCache) {
        this.rateLimitCache = rateLimitCache;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        String path = req.getRequestURI();

        // Only rate limit /api/* endpoints
        if (!path.contains("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        String key;
        int limit;

        if (path.endsWith("/api/auth/login")) {
            // Rate limit login by IP (user not authenticated yet)
            key = "login:" + getClientIp(req);
            limit = rateLimitCache.getLoginLimit();
        } else if (path.endsWith("/api/token/generate")) {
            Long userId = (Long) req.getAttribute("userId");
            if (userId == null) { chain.doFilter(request, response); return; }
            key = "token-gen:" + userId;
            limit = rateLimitCache.getTokenGenLimit();
        } else {
            Long userId = (Long) req.getAttribute("userId");
            if (userId == null) { chain.doFilter(request, response); return; }
            key = "api:" + userId;
            limit = rateLimitCache.getApiLimit();
        }

        if (!rateLimitCache.isAllowed(key, limit)) {
            log.warn("Rate limit exceeded for key: {}", key);
            res.setStatus(429);
            res.setContentType("application/json");
            res.setHeader("Retry-After", "60");
            res.setHeader("X-RateLimit-Limit", String.valueOf(limit));
            res.setHeader("X-RateLimit-Remaining", "0");
            res.getWriter().write("{\"error\":\"Rate limit exceeded. Try again in 60 seconds.\",\"status\":429}");
            return;
        }

        // Add rate limit headers
        int remaining = rateLimitCache.getRemaining(key, limit);
        res.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        res.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));

        chain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
