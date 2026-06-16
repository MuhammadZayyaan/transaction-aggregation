package com.api.transaction_aggregation.auth.session;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * Guava-based active session cache.
 * Sessions expire after a configurable period of inactivity (sliding TTL).
 * Each API call refreshes the session, keeping active users alive.
 */
@Component
public class SessionCache {

    private static final Logger log = LoggerFactory.getLogger(SessionCache.class);

    private final Cache<Long, SessionInfo> activeSessions;

    public SessionCache(@Value("${session.inactivity-timeout-minutes:15}") long timeoutMinutes) {
        this.activeSessions = CacheBuilder.newBuilder()
                .expireAfterAccess(timeoutMinutes, TimeUnit.MINUTES)
                .maximumSize(10000)
                .build();
        log.info("Session cache initialized with {} minute inactivity timeout", timeoutMinutes);
    }

    /**
     * Create or refresh a session for the given user.
     * Called on login and on every authenticated API call.
     */
    public void createOrRefreshSession(Long userId) {
        SessionInfo existing = activeSessions.getIfPresent(userId);
        if (existing != null) {
            existing.setLastActivity(LocalDateTime.now());
            activeSessions.put(userId, existing);
        } else {
            activeSessions.put(userId, new SessionInfo(userId, LocalDateTime.now()));
            log.info("New session created for user_id: {}", userId);
        }
    }

    /**
     * Check if a session exists (user is still active).
     */
    public boolean isSessionActive(Long userId) {
        return activeSessions.getIfPresent(userId) != null;
    }

    /**
     * Invalidate a session (logout).
     */
    public void invalidateSession(Long userId) {
        activeSessions.invalidate(userId);
        log.info("Session invalidated for user_id: {}", userId);
    }

    /**
     * Get session info for a user.
     */
    public SessionInfo getSession(Long userId) {
        return activeSessions.getIfPresent(userId);
    }

    /**
     * Cache user profile in the session (avoids repeated DB lookups).
     */
    public void cacheUserProfile(Long userId, com.api.transaction_aggregation.user.dto.UserProfile profile) {
        SessionInfo session = activeSessions.getIfPresent(userId);
        if (session != null) {
            session.setCachedProfile(profile);
        }
    }

    /**
     * Get cached user profile from session.
     */
    public com.api.transaction_aggregation.user.dto.UserProfile getCachedProfile(Long userId) {
        SessionInfo session = activeSessions.getIfPresent(userId);
        return (session != null && session.hasProfile()) ? session.getCachedProfile() : null;
    }

    /**
     * Get count of active sessions (for monitoring).
     */
    public long getActiveSessionCount() {
        activeSessions.cleanUp();
        return activeSessions.size();
    }
}
