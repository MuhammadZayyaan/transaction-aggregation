package com.api.transaction_aggregation.model;

import java.math.BigDecimal;

public record Transaction(
        String reference,
        String description,
        BigDecimal amount,
        String currency,
        String category,
        String timestamp,
        String type,
        String status
) {}
