package de.dreistrom.income.dto;

import java.math.BigDecimal;

/**
 * Working time compliance status per §3 ArbZG.
 * Maximum allowed working time is 48 hours per week
 * (averaged across all employment and self-employment).
 */
public record ArbZGResponse(
        int year,
        BigDecimal avgTotalHoursWeekly,
        BigDecimal maxAllowedHoursWeekly,
        boolean exceeded,
        BigDecimal avgEmploymentHoursWeekly,
        BigDecimal avgSelfEmployedHoursWeekly
) {}
