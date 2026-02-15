package de.dreistrom.expense.service;

import java.math.BigDecimal;

/**
 * One year's depreciation in a multi-year schedule.
 *
 * @param year              the calendar year
 * @param depreciation      depreciation amount for this year (pro-rata if partial)
 * @param remainingBookValue book value at end of this year
 */
public record DepreciationYearEntry(
        int year,
        BigDecimal depreciation,
        BigDecimal remainingBookValue
) {}
