package de.dreistrom.income.dto;

import java.math.BigDecimal;

/**
 * Mandatory tax filing status per §46 Abs. 2 Nr. 1 EStG.
 * Filing is required when Nebeneinkünfte (non-employment income) exceed €410.
 */
public record MandatoryFilingResponse(
        int year,
        BigDecimal nebeneinkuenfte,
        BigDecimal threshold,
        boolean filingRequired
) {}
