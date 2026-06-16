package com.api.transaction_aggregation.ratelimit;

import com.api.transaction_aggregation.role.service.RoleService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Admin endpoint to view and adjust rate limits at runtime.
 * Only accessible by ADMIN users.
 */
@RestController
@RequestMapping("/api/admin/rate-limit")
public class RateLimitController {

    private final RateLimitCache rateLimitCache;
    private final RoleService roleService;

    public RateLimitController(RateLimitCache rateLimitCache, RoleService roleService) {
        this.rateLimitCache = rateLimitCache;
        this.roleService = roleService;
    }

    @GetMapping
    public ResponseEntity<?> getCurrentLimits(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (!roleService.isAdmin(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Admin access required", "status", 403));
        }

        return ResponseEntity.ok(Map.of(
                "loginLimit", rateLimitCache.getLoginLimit(),
                "apiLimit", rateLimitCache.getApiLimit(),
                "tokenGenerateLimit", rateLimitCache.getTokenGenLimit(),
                "windowSeconds", 60
        ));
    }

    @PutMapping
    public ResponseEntity<?> updateLimits(HttpServletRequest request, @RequestBody Map<String, Integer> limits) {
        Long userId = (Long) request.getAttribute("userId");
        if (!roleService.isAdmin(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Admin access required", "status", 403));
        }

        if (limits.containsKey("loginLimit")) {
            rateLimitCache.setLoginLimit(limits.get("loginLimit"));
        }
        if (limits.containsKey("apiLimit")) {
            rateLimitCache.setApiLimit(limits.get("apiLimit"));
        }
        if (limits.containsKey("tokenGenerateLimit")) {
            rateLimitCache.setTokenGenLimit(limits.get("tokenGenerateLimit"));
        }

        return ResponseEntity.ok(Map.of(
                "message", "Rate limits updated",
                "loginLimit", rateLimitCache.getLoginLimit(),
                "apiLimit", rateLimitCache.getApiLimit(),
                "tokenGenerateLimit", rateLimitCache.getTokenGenLimit()
        ));
    }
}
