package de.dreistrom.expense.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ExpenseEntryResponse(
        Long id,
        BigDecimal amount,
        String currency,
        String category,
        LocalDate entryDate,
        Long receiptDocId,
        AllocationRuleSummary allocationRule,
        String description,
        boolean gwg,
        Instant createdAt,
        Instant updatedAt
) {}
