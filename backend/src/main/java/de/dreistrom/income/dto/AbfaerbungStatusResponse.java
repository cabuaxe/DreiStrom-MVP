package de.dreistrom.income.dto;

import java.math.BigDecimal;

public record AbfaerbungStatusResponse(
        BigDecimal ratio,
        BigDecimal gewerbeRevenue,
        BigDecimal selfEmployedRevenue,
        boolean thresholdExceeded,
        int year
) {}
