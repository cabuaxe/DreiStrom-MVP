package de.dreistrom.income.dto;

import de.dreistrom.common.domain.IncomeStream;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record IncomeEntryResponse(
        Long id,
        IncomeStream streamType,
        BigDecimal amount,
        String currency,
        LocalDate entryDate,
        String source,
        ClientSummary client,
        String description,
        Instant createdAt,
        Instant updatedAt
) {}
