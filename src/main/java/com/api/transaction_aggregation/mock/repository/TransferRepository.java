package com.api.transaction_aggregation.mock.repository;

import com.api.transaction_aggregation.mock.entity.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransferRepository extends JpaRepository<Transfer, Long> {

    List<Transfer> findByUserId(Long userId);
}
