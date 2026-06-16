package com.api.transaction_aggregation.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "sso")
public class SsoUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "failed_attempts", nullable = false)
    private Integer failedAttempts = 0;

    @Column(name = "successful_login")
    private LocalDateTime successfulLogin;

    @Column(name = "failed_login")
    private LocalDateTime failedLogin;

    public SsoUser() {}

    public SsoUser(String username, String password, Integer status) {
        this.username = username;
        this.password = password;
        this.status = status;
        this.failedAttempts = 0;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getFailedAttempts() {
        return failedAttempts;
    }

    public void setFailedAttempts(Integer failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    public LocalDateTime getSuccessfulLogin() {
        return successfulLogin;
    }

    public void setSuccessfulLogin(LocalDateTime successfulLogin) {
        this.successfulLogin = successfulLogin;
    }

    public LocalDateTime getFailedLogin() {
        return failedLogin;
    }

    public void setFailedLogin(LocalDateTime failedLogin) {
        this.failedLogin = failedLogin;
    }
}
