package com.api.transaction_aggregation.role.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "role")
public class UserRole {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "role", nullable = false)
    private String role;

    public UserRole() {}

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
