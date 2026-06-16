package com.api.transaction_aggregation.auth.repository;

import com.api.transaction_aggregation.auth.entity.SsoUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SsoUserRepository extends JpaRepository<SsoUser, Long> {

    Optional<SsoUser> findByUsername(String username);
}
