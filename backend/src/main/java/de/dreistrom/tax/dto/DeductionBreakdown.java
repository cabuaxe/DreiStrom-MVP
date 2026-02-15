package de.dreistrom.tax.dto;

import java.math.BigDecimal;

/**
 * Breakdown of deductions applied before progressive tax computation.
 * All amounts in EUR.
 */
public record DeductionBreakdown(
        BigDecimal businessExpensesFreiberuf,
        BigDecimal businessExpensesGewerbe,
        BigDecimal werbungskostenpauschale,
        BigDecimal sonderausgabenpauschale,
        BigDecimal totalDeductions
) {}
