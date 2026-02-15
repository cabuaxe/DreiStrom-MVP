package de.dreistrom.expense.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateExpenseEntryRequest(
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank @Size(max = 100) String category,
        @NotNull LocalDate entryDate,
        Long allocationRuleId,
        Long receiptDocId,
        @Size(max = 500) String description
) {}
