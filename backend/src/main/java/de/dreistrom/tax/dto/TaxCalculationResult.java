package de.dreistrom.tax.dto;

import java.math.BigDecimal;

/**
 * Result of a full income tax calculation per §32a EStG.
 * Includes progressive tax, Solidaritaetszuschlag, deductions, and effective rate.
 * All monetary amounts in EUR.
 */
public record TaxCalculationResult(
        int taxYear,

        // ── Gross income by stream ──────────────────────────────────────
        BigDecimal employmentIncome,
        BigDecimal freiberufIncome,
        BigDecimal gewerbeIncome,
        BigDecimal totalGrossIncome,

        // ── Deductions ──────────────────────────────────────────────────
        DeductionBreakdown deductions,

        // ── Taxable income (zu versteuerndes Einkommen) ─────────────────
        BigDecimal taxableIncome,

        // ── Tax amounts ─────────────────────────────────────────────────
        BigDecimal incomeTax,
        BigDecimal solidaritaetszuschlag,
        BigDecimal totalTax,

        // ── Rates ───────────────────────────────────────────────────────
        BigDecimal marginalRate,
        BigDecimal effectiveRate
) {}
