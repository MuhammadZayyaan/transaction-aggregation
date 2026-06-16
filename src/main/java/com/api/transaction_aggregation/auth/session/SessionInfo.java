package com.api.transaction_aggregation.auth.session;

import com.api.transaction_aggregation.user.dto.UserProfile;

import java.time.LocalDateTime;

/**
 * Holds session metadata and cached user profile for an active user.
 */
public class SessionInfo {

    private final Long userId;
    private final LocalDateTime createdAt;
    private LocalDateTime lastActivity;
    private UserProfile cachedProfile;

    public SessionInfo(Long userId, LocalDateTime lastActivity) {
        this.userId = userId;
        this.createdAt = LocalDateTime.now();
        this.lastActivity = lastActivity;
    }

    public Long getUserId() { return userId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastActivity() { return lastActivity; }
    public void setLastActivity(LocalDateTime lastActivity) { this.lastActivity = lastActivity; }

    public UserProfile getCachedProfile() { return cachedProfile; }
    public void setCachedProfile(UserProfile profile) { this.cachedProfile = profile; }
    public boolean hasProfile() { return cachedProfile != null; }
}
