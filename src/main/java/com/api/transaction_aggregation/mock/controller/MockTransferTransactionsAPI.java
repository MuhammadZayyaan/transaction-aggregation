package com.api.transaction_aggregation.mock.controller;

import com.api.transaction_aggregation.mock.entity.Transfer;
import com.api.transaction_aggregation.mock.repository.TransferRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Simulates an external Transfer Transactions API.
 * In production, this would be a separate microservice.
 */
@RestController
@RequestMapping("/mock/api/transfers")
public class MockTransferTransactionsAPI {

    private final TransferRepository transferRepository;

    public MockTransferTransactionsAPI(TransferRepository transferRepository) {
        this.transferRepository = transferRepository;
    }

    @GetMapping("/{userId}")
    public List<Transfer> getTransfersByUser(@PathVariable Long userId) {
        return transferRepository.findByUserId(userId);
    }

    @GetMapping
    public List<Transfer> getAllTransfers() {
        return transferRepository.findAll();
    }
}
