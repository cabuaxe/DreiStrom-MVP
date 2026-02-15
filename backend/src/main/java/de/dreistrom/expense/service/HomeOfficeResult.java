package de.dreistrom.expense.service;

import java.math.BigDecimal;

/**
 * Result of a home office deduction calculation.
 *
 * @param method         the calculation method used
 * @param deduction      the deductible amount in EUR
 * @param details        human-readable breakdown of the calculation
 */
public record HomeOfficeResult(
        HomeOfficeMethod method,
        BigDecimal deduction,
        String details
) {}
