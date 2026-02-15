package de.dreistrom.tax.dto;

import java.math.BigDecimal;

/**
 * Recommended monthly transfer to the tax reserve account.
 * Based on current net self-employed profit and configurable reserve rate (25-35%).
 */
public record TaxReserveRecommendation(
        int year,
        BigDecimal netSelfEmployedProfit,
        BigDecimal reserveRatePercent,
        BigDecimal monthlyReserve,
        BigDecimal annualReserve,
        BigDecimal alreadyReserved,
        BigDecimal remainingToReserve,
        int monthsRemaining
) {}
