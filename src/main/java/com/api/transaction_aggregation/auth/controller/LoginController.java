package com.api.transaction_aggregation.auth.controller;

import com.api.transaction_aggregation.auth.dto.LoginRequest;
import com.api.transaction_aggregation.auth.dto.LoginResponse;
import com.api.transaction_aggregation.auth.service.JwtService;
import com.api.transaction_aggregation.auth.service.LoginService;
import com.api.transaction_aggregation.auth.session.SessionCache;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class LoginController {

    private final LoginService loginService;
    private final JwtService jwtService;
    private final SessionCache sessionCache;

    public LoginController(LoginService loginService, JwtService jwtService, SessionCache sessionCache) {
        this.loginService = loginService;
        this.jwtService = jwtService;
        this.sessionCache = sessionCache;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = loginService.validate(request);

        if (!response.success()) {
            return ResponseEntity.status(401).body(response);
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtService.isTokenValid(token)) {
                Long userId = jwtService.extractUserId(token);
                sessionCache.invalidateSession(userId);
                return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
            }
        }

        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }
}
