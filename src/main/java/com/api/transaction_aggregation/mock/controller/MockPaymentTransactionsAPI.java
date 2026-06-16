package com.api.transaction_aggregation.mock.controller;

import com.api.transaction_aggregation.mock.entity.Payment;
import com.api.transaction_aggregation.mock.repository.PaymentRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Simulates an external Payment Transactions API.
 * In production, this would be a separate microservice.
 */
@RestController
@RequestMapping("/mock/api/payments")
public class MockPaymentTransactionsAPI {

    private final PaymentRepository paymentRepository;

    public MockPaymentTransactionsAPI(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @GetMapping("/{userId}")
    public List<Payment> getPaymentsByUser(@PathVariable Long userId) {
        return paymentRepository.findByUserId(userId);
    }

    @GetMapping
    public List<Payment> getAllPayments() {
        return paymentRepository.findAll();
    }
}
