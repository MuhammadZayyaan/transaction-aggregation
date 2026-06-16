package com.api.transaction_aggregation.controller;

import com.api.transaction_aggregation.model.Transaction;
import com.api.transaction_aggregation.service.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    public ResponseEntity<?> getAll(
            HttpServletRequest request,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false, defaultValue = "timestamp") String sort,
            @RequestParam(required = false, defaultValue = "desc") String order) {

        Long userId = (Long) request.getAttribute("userId");
        List<Transaction> transactions = transactionService.getAllTransactions(userId);
        transactions = applyFilters(transactions, category, status, from, to, minAmount, maxAmount);
        transactions = applySort(transactions, sort, order);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/payments")
    public ResponseEntity<?> getPayments(
            HttpServletRequest request,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false, defaultValue = "timestamp") String sort,
            @RequestParam(required = false, defaultValue = "desc") String order) {

        Long userId = (Long) request.getAttribute("userId");
        List<Transaction> transactions = transactionService.getPaymentTransactions(userId);
        transactions = applyFilters(transactions, category, status, from, to, minAmount, maxAmount);
        transactions = applySort(transactions, sort, order);
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/transfers")
    public ResponseEntity<?> getTransfers(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ResponseEntity.ok(transactionService.getRawTransfers(userId));
    }

    @GetMapping("/aggregate")
    public ResponseEntity<?> aggregate(
            HttpServletRequest request,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount) {

        Long userId = (Long) request.getAttribute("userId");
        List<Transaction> transactions = transactionService.getAllTransactions(userId);
        transactions = applyFilters(transactions, category, status, from, to, minAmount, maxAmount);
        return ResponseEntity.ok(transactionService.aggregateFromList(transactions));
    }

    private List<Transaction> applyFilters(List<Transaction> transactions,
                                           String category, String status,
                                           String from, String to,
                                           BigDecimal minAmount, BigDecimal maxAmount) {

        return transactions.stream()
                .filter(t -> category == null || category.equalsIgnoreCase(t.category()))
                .filter(t -> status == null || status.equalsIgnoreCase(t.status()))
                .filter(t -> from == null || (t.timestamp() != null && t.timestamp().compareTo(from) >= 0))
                .filter(t -> to == null || (t.timestamp() != null && t.timestamp().compareTo(to) <= 0))
                .filter(t -> minAmount == null || t.amount().compareTo(minAmount) >= 0)
                .filter(t -> maxAmount == null || t.amount().compareTo(maxAmount) <= 0)
                .toList();
    }

    private List<Transaction> applySort(List<Transaction> transactions, String sort, String order) {
        Comparator<Transaction> comparator = switch (sort) {
            case "amount" -> Comparator.comparing(Transaction::amount);
            case "category" -> Comparator.comparing(t -> t.category() != null ? t.category() : "");
            case "status" -> Comparator.comparing(Transaction::status);
            case "reference" -> Comparator.comparing(Transaction::reference);
            default -> Comparator.comparing(t -> t.timestamp() != null ? t.timestamp() : "");
        };

        if ("desc".equalsIgnoreCase(order)) {
            comparator = comparator.reversed();
        }

        return transactions.stream().sorted(comparator).toList();
    }
}
