package com.api.transaction_aggregation.role.service;

import com.api.transaction_aggregation.role.entity.UserRole;
import com.api.transaction_aggregation.role.repository.UserRoleRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RoleService {

    private final UserRoleRepository userRoleRepository;

    public RoleService(UserRoleRepository userRoleRepository) {
        this.userRoleRepository = userRoleRepository;
    }

    public String getRoleByUserId(Long userId) {
        Optional<UserRole> optionalRole = userRoleRepository.findByUserId(userId);
        return optionalRole.map(UserRole::getRole).orElse("USER");
    }

    public boolean isAdmin(Long userId) {
        return "ADMIN".equals(getRoleByUserId(userId));
    }
}
