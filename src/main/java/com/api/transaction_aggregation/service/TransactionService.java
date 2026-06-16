package com.api.transaction_aggregation.service;

import com.api.transaction_aggregation.model.AggregationResult;
import com.api.transaction_aggregation.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Aggregates transaction data by calling the mock Payment and Transfer APIs.
 * The base URL is configurable so it can point to real services in production.
 */
@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final RestClient restClient;
    private final String paymentsBaseUrl;
    private final String transfersBaseUrl;

    public TransactionService(
            @Value("${mock.api.payments-url}") String paymentsBaseUrl,
            @Value("${mock.api.transfers-url}") String transfersBaseUrl) {
        this.restClient = RestClient.create();
        this.paymentsBaseUrl = paymentsBaseUrl;
        this.transfersBaseUrl = transfersBaseUrl;
    }

    public List<Transaction> getAllTransactions(Long userId) {
        List<Transaction> all = new ArrayList<>();
        all.addAll(getPayments(userId));
        all.addAll(getTransfers(userId));
        return all;
    }

    public List<Transaction> getPaymentTransactions(Long userId) {
        return getPayments(userId);
    }

    public List<Transaction> getTransferTransactions(Long userId) {
        return getTransfers(userId);
    }

    public List<Map<String, Object>> getRawTransfers(Long userId) {
        try {
            String url = transfersBaseUrl + "/" + userId;
            List<Map<String, Object>> transfers = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return transfers != null ? transfers : List.of();
        } catch (Exception e) {
            log.error("Failed to call transfers API for raw data: {}", e.getMessage());
            return List.of();
        }
    }

    public AggregationResult aggregate(Long userId) {
        List<Transaction> transactions = getAllTransactions(userId);
        return aggregateFromList(transactions);
    }

    public AggregationResult aggregateFromList(List<Transaction> transactions) {
        int total = transactions.size();

        BigDecimal totalAmount = transactions.stream()
                .map(Transaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> byCategory = transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.category() != null ? t.category() : "uncategorized",
                        Collectors.reducing(BigDecimal.ZERO, Transaction::amount, BigDecimal::add)
                ));

        return new AggregationResult(total, totalAmount, byCategory);
    }

    private List<Transaction> getPayments(Long userId) {
        try {
            String url = paymentsBaseUrl + "/" + userId;
            log.debug("Calling payments API: {}", url);

            List<Map<String, Object>> payments = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (payments == null) return List.of();

            return payments.stream()
                    .map(p -> new Transaction(
                            (String) p.get("reference"),
                            (String) p.get("description"),
                            new BigDecimal(p.get("amount").toString()),
                            (String) p.get("currency"),
                            (String) p.get("category"),
                            (String) p.get("timestamp"),
                            "payment",
                            (String) p.get("status")
                    ))
                    .toList();
        } catch (Exception e) {
            log.error("Failed to call payments API: {}", e.getMessage());
            return List.of();
        }
    }

    private List<Transaction> getTransfers(Long userId) {
        try {
            String url = transfersBaseUrl + "/" + userId;
            log.debug("Calling transfers API: {}", url);

            List<Map<String, Object>> transfers = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (transfers == null) return List.of();

            return transfers.stream()
                    .map(t -> new Transaction(
                            (String) t.get("reference"),
                            (String) t.get("description"),
                            new BigDecimal(t.get("amount").toString()),
                            (String) t.get("currency"),
                            "transfer",
                            (String) t.get("timestamp"),
                            "transfer",
                            (String) t.get("status")
                    ))
                    .toList();
        } catch (Exception e) {
            log.error("Failed to call transfers API: {}", e.getMessage());
            return List.of();
        }
    }
}
