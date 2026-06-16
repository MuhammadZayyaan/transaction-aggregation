package com.api.transaction_aggregation.auth.controller;

import com.api.transaction_aggregation.auth.service.ApiTokenService;
import com.api.transaction_aggregation.role.service.RoleService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint for generating API tokens (certificate-signed).
 * Only accessible by authenticated ADMIN users.
 */
@RestController
@RequestMapping("/api/token")
public class ApiTokenController {

    private final ApiTokenService apiTokenService;
    private final RoleService roleService;

    public ApiTokenController(ApiTokenService apiTokenService, RoleService roleService) {
        this.apiTokenService = apiTokenService;
        this.roleService = roleService;
    }

    /**
     * Generate an API token for the authenticated user.
     * Only ADMIN users can generate API tokens.
     */
    @PostMapping("/generate")
    public ResponseEntity<?> generateApiToken(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String username = (String) request.getAttribute("username");

        // Only admins can generate API tokens
        String role = roleService.getRoleByUserId(userId);
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Only admins can generate API tokens", "status", 403));
        }

        String apiToken = apiTokenService.generateApiToken(userId, username, role);

        return ResponseEntity.ok(Map.of(
                "apiToken", apiToken,
                "tokenType", "api",
                "expiresIn", "60 minutes",
                "signedWith", "RSA (PKCS12 certificate)"
        ));
    }

    /**
     * Validate an API token — useful for external services to verify a token.
     */
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(HttpServletRequest request) {
        // The filter already validated the session token to reach here.
        // This endpoint lets you test API token validation separately.
        String authHeader = request.getHeader("X-API-Token");

        if (authHeader == null || authHeader.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Provide API token in X-API-Token header"));
        }

        boolean valid = apiTokenService.isApiTokenValid(authHeader);

        if (valid) {
            String username = apiTokenService.extractUsername(authHeader);
            Long userId = apiTokenService.extractUserId(authHeader);
            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "username", username,
                    "userId", userId,
                    "tokenType", apiTokenService.extractTokenType(authHeader)
            ));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("valid", false, "error", "Invalid or expired API token"));
    }
}
