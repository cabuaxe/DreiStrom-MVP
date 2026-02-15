package de.dreistrom.expense.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAllocationRuleRequest(
        @NotBlank @Size(max = 255) String name,
        short freiberufPct,
        short gewerbePct,
        short personalPct
) {}
