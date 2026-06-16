package com.api.transaction_aggregation.model;

import java.math.BigDecimal;
import java.util.Map;

public record AggregationResult(
        int totalTransactions,
        BigDecimal totalAmount,
        Map<String, BigDecimal> amountByCategory
) {}
