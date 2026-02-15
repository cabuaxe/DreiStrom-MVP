package de.dreistrom.income.dto;

import java.math.BigDecimal;

public record GewerbesteuerThresholdResponse(
        int year,
        BigDecimal gewerbeProfit,
        BigDecimal freibetrag,
        boolean freibetragExceeded,
        BigDecimal gewerbeRevenue,
        BigDecimal bilanzierungRevenueThreshold,
        boolean bilanzierungRevenueExceeded,
        BigDecimal bilanzierungProfitThreshold,
        boolean bilanzierungProfitExceeded
) {}
