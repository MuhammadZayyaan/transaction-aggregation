package com.api.transaction_aggregation.auth.service;

import com.api.transaction_aggregation.auth.dto.LoginRequest;
import com.api.transaction_aggregation.auth.dto.LoginResponse;
import com.api.transaction_aggregation.auth.entity.SsoUser;
import com.api.transaction_aggregation.auth.repository.SsoUserRepository;
import com.api.transaction_aggregation.auth.session.SessionCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class LoginService {

    private static final Logger log = LoggerFactory.getLogger(LoginService.class);

    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_LOCKED = 2;

    private final SsoUserRepository ssoUserRepository;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SessionCache sessionCache;
    private final int maxFailedAttempts;

    public LoginService(SsoUserRepository ssoUserRepository,
                        JwtService jwtService,
                        BCryptPasswordEncoder passwordEncoder,
                        SessionCache sessionCache,
                        @Value("${login.max-failed-attempts}") int maxFailedAttempts) {
        this.ssoUserRepository = ssoUserRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.sessionCache = sessionCache;
        this.maxFailedAttempts = maxFailedAttempts;
    }

    @Transactional
    public LoginResponse validate(LoginRequest request) {
        // Step 1: Retrieve user by username
        Optional<SsoUser> optionalUser = ssoUserRepository.findByUsername(request.username());
        if (optionalUser.isEmpty()) {
            log.warn("Login attempt for non-existent user: {}", request.username());
            return LoginResponse.failed("Invalid credentials");
        }

        SsoUser user = optionalUser.get();

        // Step 2: Check if user status is active
        if (user.getStatus() != STATUS_ACTIVE) {
            log.warn("Login attempt for locked/disabled user: {}", request.username());
            return LoginResponse.failed("Invalid credentials");
        }

        // Step 3: Encrypt captured password and compare with stored hash
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            // Password mismatch — handle failed attempt
            return handleFailedAttempt(user);
        }

        // Step 4: Successful login — reset failed attempts, update timestamp
        LocalDateTime previousLogin = user.getSuccessfulLogin();
        user.setFailedAttempts(0);
        user.setSuccessfulLogin(LocalDateTime.now());
        ssoUserRepository.save(user);

        // Step 5: Generate JWT token (include last login time)
        String token = jwtService.generateToken(user.getUserId(), previousLogin);
        log.info("Successful login for user: {}", request.username());

        // Step 6: Create session in cache
        sessionCache.createOrRefreshSession(user.getUserId());

        return LoginResponse.success(token, user.getUsername());
    }

    private LoginResponse handleFailedAttempt(SsoUser user) {
        int newFailedAttempts = user.getFailedAttempts() + 1;
        user.setFailedAttempts(newFailedAttempts);
        user.setFailedLogin(LocalDateTime.now());

        // Check if failed attempts exceed threshold — lock the account
        if (newFailedAttempts >= maxFailedAttempts) {
            user.setStatus(STATUS_LOCKED);
            ssoUserRepository.save(user);
            log.warn("Account locked due to {} failed attempts: {}", newFailedAttempts, user.getUsername());
            return LoginResponse.failed("Invalid credentials");
        }

        ssoUserRepository.save(user);
        log.warn("Failed login attempt {} for user: {}", newFailedAttempts, user.getUsername());
        return LoginResponse.failed("Invalid credentials");
    }
}
