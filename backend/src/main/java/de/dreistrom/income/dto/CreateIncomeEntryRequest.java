package de.dreistrom.income.dto;

import de.dreistrom.common.domain.IncomeStream;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateIncomeEntryRequest(
        @NotNull IncomeStream streamType,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotNull LocalDate entryDate,
        String source,
        Long clientId,
        @Size(max = 500) String description
) {}
