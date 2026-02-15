package de.dreistrom.expense.dto;

public record AllocationRuleSummary(
        Long id,
        String name,
        short freiberufPct,
        short gewerbePct,
        short personalPct
) {}
