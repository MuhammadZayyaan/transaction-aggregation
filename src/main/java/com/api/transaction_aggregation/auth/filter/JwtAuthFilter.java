package com.api.transaction_aggregation.auth.filter;

import com.api.transaction_aggregation.auth.service.ApiTokenService;
import com.api.transaction_aggregation.auth.service.JwtService;
import com.api.transaction_aggregation.auth.session.SessionCache;
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
 * JWT authentication filter that intercepts all /api/** requests (except /api/auth/login).
 * Supports two token types:
 * 1. Session tokens (HMAC-signed) — validated against Guava session cache
 * 2. API tokens (RSA-signed) — validated via public key, no session required
 */
public class JwtAuthFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    private final JwtService jwtService;
    private final ApiTokenService apiTokenService;
    private final SessionCache sessionCache;

    public JwtAuthFilter(JwtService jwtService, ApiTokenService apiTokenService, SessionCache sessionCache) {
        this.jwtService = jwtService;
        this.apiTokenService = apiTokenService;
        this.sessionCache = sessionCache;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();

        // Skip auth for login endpoint and non-API resources
        if (path.endsWith("/api/auth/login") || path.endsWith("/api/auth/logout") || !path.contains("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = httpRequest.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorized(httpResponse, "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7);

        // Try 1: Validate as API token (RSA-signed, no session required)
        if (apiTokenService.isApiTokenValid(token)) {
            Long userId = apiTokenService.extractUserId(token);
            httpRequest.setAttribute("userId", userId);
            httpRequest.setAttribute("tokenType", "api");
            log.debug("API token authenticated for user_id: {}", userId);
            chain.doFilter(request, response);
            return;
        }

        // Try 2: Validate as session token (HMAC-signed, requires active session)
        if (!jwtService.isTokenValid(token)) {
            sendUnauthorized(httpResponse, "Token expired or invalid");
            return;
        }

        Long userId = jwtService.extractUserId(token);

        // Check session exists in cache (guards against inactivity timeout)
        if (!sessionCache.isSessionActive(userId)) {
            log.warn("Session expired for user_id: {}. Token valid but session inactive.", userId);
            sendUnauthorized(httpResponse, "Session expired. Please log in again.");
            return;
        }

        // Refresh session TTL (sliding window)
        sessionCache.createOrRefreshSession(userId);

        // Set user context for downstream controllers
        httpRequest.setAttribute("userId", userId);
        httpRequest.setAttribute("tokenType", "session");

        log.debug("Session token authenticated for user_id: {}", userId);

        chain.doFilter(request, response);
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\",\"status\":401}");
    }
}
