package de.dreistrom.expense.dto;

import java.time.Instant;

public record AllocationRuleResponse(
        Long id,
        String name,
        short freiberufPct,
        short gewerbePct,
        short personalPct,
        Instant createdAt,
        Instant updatedAt
) {}
