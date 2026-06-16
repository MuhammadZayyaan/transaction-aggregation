package com.api.transaction_aggregation.auth.controller;

import com.api.transaction_aggregation.auth.entity.SsoUser;
import com.api.transaction_aggregation.auth.repository.SsoUserRepository;
import com.api.transaction_aggregation.role.service.RoleService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

/**
 * Admin endpoint to unlock user accounts.
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final SsoUserRepository ssoUserRepository;
    private final RoleService roleService;

    public AdminUserController(SsoUserRepository ssoUserRepository, RoleService roleService) {
        this.ssoUserRepository = ssoUserRepository;
        this.roleService = roleService;
    }

    @PostMapping("/{username}/unlock")
    public ResponseEntity<?> unlockUser(HttpServletRequest request, @PathVariable String username) {
        Long userId = (Long) request.getAttribute("userId");

        if (!roleService.isAdmin(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Admin access required", "status", 403));
        }

        Optional<SsoUser> optionalUser = ssoUserRepository.findByUsername(username);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found: " + username, "status", 404));
        }

        SsoUser user = optionalUser.get();
        user.setStatus(1);
        user.setFailedAttempts(0);
        ssoUserRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "User unlocked successfully",
                "username", username,
                "status", 1,
                "failedAttempts", 0
        ));
    }
}
