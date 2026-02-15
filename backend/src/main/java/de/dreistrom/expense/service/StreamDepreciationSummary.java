package de.dreistrom.expense.service;

import java.math.BigDecimal;

/**
 * Per-stream depreciation totals for a given year.
 */
public record StreamDepreciationSummary(
        BigDecimal freiberuf,
        BigDecimal gewerbe,
        BigDecimal personal,
        BigDecimal total
) {}
