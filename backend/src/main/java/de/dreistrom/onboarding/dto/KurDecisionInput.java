package de.dreistrom.onboarding.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record KurDecisionInput(
        @NotNull @PositiveOrZero BigDecimal projectedFreiberufRevenue,
        @NotNull @PositiveOrZero BigDecimal projectedGewerbeRevenue,
        @NotNull @PositiveOrZero BigDecimal projectedBusinessExpenses,
        @NotNull @PositiveOrZero Integer b2bClientCount,
        @NotNull @PositiveOrZero Integer b2cClientCount
) {}
