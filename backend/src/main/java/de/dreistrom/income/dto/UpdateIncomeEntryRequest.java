package de.dreistrom.income.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateIncomeEntryRequest(
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotNull LocalDate entryDate,
        String source,
        Long clientId,
        @Size(max = 500) String description
) {}
