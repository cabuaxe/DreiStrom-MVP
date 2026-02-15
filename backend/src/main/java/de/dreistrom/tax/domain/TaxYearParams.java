package de.dreistrom.tax.domain;

import java.math.BigDecimal;

/**
 * Year-specific parameters for the §32a EStG progressive income tax formula.
 * Each tax year has its own bracket thresholds and formula coefficients,
 * published annually by the Bundesfinanzministerium.
 */
public record TaxYearParams(
        int year,

        // ── Zone boundaries (zvE thresholds in EUR) ─────────────────────
        BigDecimal grundfreibetrag,       // Zone 1 upper limit (tax-free)
        BigDecimal zone2Upper,            // Zone 2 upper limit
        BigDecimal zone3Upper,            // Zone 3 upper limit
        BigDecimal zone4Upper,            // Zone 4 upper limit (Reichensteuer start)

        // ── Zone 2 quadratic: (a·y + b)·y  where y = (zvE - grundfreibetrag) / 10_000
        BigDecimal zone2A,
        BigDecimal zone2B,

        // ── Zone 3 quadratic: (a·z + b)·z + c  where z = (zvE - zone2Upper) / 10_000
        BigDecimal zone3A,
        BigDecimal zone3B,
        BigDecimal zone3C,

        // ── Zone 4 linear: rate·zvE - subtraction
        BigDecimal zone4Rate,
        BigDecimal zone4Sub,

        // ── Zone 5 linear: rate·zvE - subtraction
        BigDecimal zone5Rate,
        BigDecimal zone5Sub,

        // ── Solidaritaetszuschlag ───────────────────────────────────────
        BigDecimal soliRate,              // 5.5%
        BigDecimal soliExemption,         // ESt threshold below which no Soli
        BigDecimal soliMilderungsRate,    // 11.9% glide zone factor

        // ── Standard deductions ─────────────────────────────────────────
        BigDecimal werbungskostenpauschale,   // Employment income deduction
        BigDecimal sonderausgabenpauschale    // Special expenses flat deduction
) {

    /** §32a EStG Veranlagungszeitraum 2024. */
    public static TaxYearParams of2024() {
        return new TaxYearParams(
                2024,
                new BigDecimal("11604"),
                new BigDecimal("17005"),
                new BigDecimal("66760"),
                new BigDecimal("277825"),
                new BigDecimal("922.98"),
                new BigDecimal("1400"),
                new BigDecimal("181.19"),
                new BigDecimal("2397"),
                new BigDecimal("1025.38"),
                new BigDecimal("0.42"),
                new BigDecimal("10602.13"),
                new BigDecimal("0.45"),
                new BigDecimal("18936.88"),
                new BigDecimal("0.055"),
                new BigDecimal("18130"),
                new BigDecimal("0.119"),
                new BigDecimal("1230"),
                new BigDecimal("36")
        );
    }

    /** §32a EStG Veranlagungszeitraum 2025. */
    public static TaxYearParams of2025() {
        return new TaxYearParams(
                2025,
                new BigDecimal("12084"),
                new BigDecimal("17430"),
                new BigDecimal("68430"),
                new BigDecimal("277825"),
                new BigDecimal("933.52"),
                new BigDecimal("1400"),
                new BigDecimal("176.64"),
                new BigDecimal("2397"),
                new BigDecimal("1015.13"),
                new BigDecimal("0.42"),
                new BigDecimal("10586.26"),
                new BigDecimal("0.45"),
                new BigDecimal("18919.49"),
                new BigDecimal("0.055"),
                new BigDecimal("19950"),
                new BigDecimal("0.119"),
                new BigDecimal("1230"),
                new BigDecimal("36")
        );
    }

    /**
     * Resolve parameters for a given tax year.
     * Falls back to the most recent available year.
     */
    public static TaxYearParams forYear(int year) {
        return switch (year) {
            case 2024 -> of2024();
            default -> of2025(); // 2025+ uses latest available params
        };
    }
}
