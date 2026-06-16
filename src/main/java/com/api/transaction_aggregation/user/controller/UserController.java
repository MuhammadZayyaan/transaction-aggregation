package com.api.transaction_aggregation.user.controller;

import com.api.transaction_aggregation.auth.session.SessionCache;
import com.api.transaction_aggregation.role.service.RoleService;
import com.api.transaction_aggregation.user.dto.UserProfile;
import com.api.transaction_aggregation.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;
    private final RoleService roleService;
    private final SessionCache sessionCache;

    public UserController(UserService userService, RoleService roleService, SessionCache sessionCache) {
        this.userService = userService;
        this.roleService = roleService;
        this.sessionCache = sessionCache;
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");

        // Check session cache first — avoid DB hit on repeated calls
        UserProfile cached = sessionCache.getCachedProfile(userId);
        if (cached != null) {
            log.debug("Returning cached profile for user_id: {}", userId);
            return ResponseEntity.ok(cached);
        }

        // First call — load from DB
        UserProfile profile = userService.getUserByUserId(userId);
        if (profile == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User profile not found");
        }

        // Add role
        String role = roleService.getRoleByUserId(userId);
        UserProfile fullProfile = profile.withRole(role);

        // Cache in session for subsequent calls
        sessionCache.cacheUserProfile(userId, fullProfile);
        log.debug("Loaded and cached profile for user_id: {}", userId);

        return ResponseEntity.ok(fullProfile);
    }
}
