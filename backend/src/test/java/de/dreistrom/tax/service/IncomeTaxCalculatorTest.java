package de.dreistrom.tax.service;

import de.dreistrom.tax.domain.TaxYearParams;
import de.dreistrom.tax.dto.TaxCalculationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the §32a EStG progressive tax calculator.
 * Reference values verified against the BMF Einkommensteuer-Rechner.
 */
class IncomeTaxCalculatorTest {

    private IncomeTaxCalculator calculator;
    private TaxYearParams params2024;
    private TaxYearParams params2025;

    @BeforeEach
    void setUp() {
        calculator = new IncomeTaxCalculator();
        params2024 = TaxYearParams.of2024();
        params2025 = TaxYearParams.of2025();
    }

    // ── Zone 1: Grundfreibetrag (no tax) ────────────────────────────────

    @Nested
    class Zone1Grundfreibetrag {

        @Test
        void zeroIncome() {
            BigDecimal tax = calculator.computeProgressiveTax(params2024, BigDecimal.ZERO);
            assertThat(tax).isEqualByComparingTo("0.00");
        }

        @Test
        void belowGrundfreibetrag2024() {
            BigDecimal tax = calculator.computeProgressiveTax(params2024, new BigDecimal("10000"));
            assertThat(tax).isEqualByComparingTo("0.00");
        }

        @Test
        void exactGrundfreibetrag2024() {
            BigDecimal tax = calculator.computeProgressiveTax(params2024, new BigDecimal("11604"));
            assertThat(tax).isEqualByComparingTo("0.00");
        }

        @Test
        void exactGrundfreibetrag2025() {
            BigDecimal tax = calculator.computeProgressiveTax(params2025, new BigDecimal("12084"));
            assertThat(tax).isEqualByComparingTo("0.00");
        }

        @Test
        void negativeIncome() {
            BigDecimal tax = calculator.computeProgressiveTax(params2024, new BigDecimal("-5000"));
            assertThat(tax).isEqualByComparingTo("0.00");
        }
    }

    // ── Zone 2: Entry rate (14% rising) ─────────────────────────────────

    @Nested
    class Zone2ProgressiveEntry {

        @Test
        void justAboveGrundfreibetrag2024() {
            // zvE = 12,000 → y = (12000 - 11604) / 10000 = 0.0396
            // ESt = (922.98 * 0.0396 + 1400) * 0.0396 = (36.55 + 1400) * 0.0396 ≈ 56.85
            // Truncated to 56
            BigDecimal tax = calculator.computeProgressiveTax(params2024, new BigDecimal("12000"));
            assertThat(tax).isEqualByComparingTo("56.00");
        }

        @Test
        void midZone2_2024_15000() {
            // zvE = 15,000 → y = (15000-11604)/10000 = 0.3396
            // ESt = (922.98 * 0.3396 + 1400) * 0.3396 = 1713.44 * 0.3396 ≈ 581
            BigDecimal tax = calculator.computeProgressiveTax(params2024, new BigDecimal("15000"));
            assertThat(tax).isEqualByComparingTo("581.00");
        }

        @Test
        void atZone2Upper2024() {
            // zvE = 17,005 → y = (17005-11604)/10000 = 0.5401
            // ESt = (922.98 * 0.5401 + 1400) * 0.5401 ≈ 1025
            BigDecimal tax = calculator.computeProgressiveTax(params2024, new BigDecimal("17005"));
            assertThat(tax).isEqualByComparingTo("1025.00");
        }
    }

    // ── Zone 3: Progressive middle bracket ──────────────────────────────

    @Nested
    class Zone3ProgressiveMiddle {

        @Test
        void at30000_2024() {
            // zvE = 30,000 → z = (30000-17005)/10000 = 1.2995
            // ESt = (181.19 * 1.2995 + 2397) * 1.2995 + 1025.38 ≈ 4446
            BigDecimal tax = calculator.computeProgressiveTax(params2024, new BigDecimal("30000"));
            assertThat(tax).isEqualByComparingTo("4446.00");
        }

        @Test
        void at50000_2024() {
            // zvE = 50,000 → z = (50000-17005)/10000 = 3.2995
            // ESt = (181.19 * 3.2995 + 2397) * 3.2995 + 1025.38 ≈ 10906
            BigDecimal tax = calculator.computeProgressiveTax(params2024, new BigDecimal("50000"));
            assertThat(tax).isEqualByComparingTo("10906.00");
        }

        @Test
        void atZone3Upper2024() {
            // zvE = 66,760 → z = (66760-17005)/10000 = 4.9755
            // ESt ≈ 17437 (matches zone 4 entry: 0.42*66761 - 10602.13 ≈ 17437)
            BigDecimal tax = calculator.computeProgressiveTax(params2024, new BigDecimal("66760"));
            assertThat(tax).isEqualByComparingTo("17437.00");
        }
    }

    // ── Zone 4: 42% Spitzensteuersatz ───────────────────────────────────

    @Nested
    class Zone4Spitzensteuersatz {

        @Test
        void at80000_2024() {
            // ESt = 0.42 * 80000 - 10602.13 = 33600 - 10602.13 = 22997.87 → 22997
            BigDecimal tax = calculator.computeProgressiveTax(params2024, new BigDecimal("80000"));
            assertThat(tax).isEqualByComparingTo("22997.00");
        }

        @Test
        void at100000_2024() {
            // ESt = 0.42 * 100000 - 10602.13 = 42000 - 10602.13 = 31397.87 → 31397
            BigDecimal tax = calculator.computeProgressiveTax(params2024, new BigDecimal("100000"));
            assertThat(tax).isEqualByComparingTo("31397.00");
        }

        @Test
        void at200000_2024() {
            // ESt = 0.42 * 200000 - 10602.13 = 84000 - 10602.13 = 73397.87 → 73397
            BigDecimal tax = calculator.computeProgressiveTax(params2024, new BigDecimal("200000"));
            assertThat(tax).isEqualByComparingTo("73397.00");
        }
    }

    // ── Zone 5: 45% Reichensteuer ───────────────────────────────────────

    @Nested
    class Zone5Reichensteuer {

        @Test
        void at300000_2024() {
            // ESt = 0.45 * 300000 - 18936.88 = 135000 - 18936.88 = 116063.12 → 116063
            BigDecimal tax = calculator.computeProgressiveTax(params2024, new BigDecimal("300000"));
            assertThat(tax).isEqualByComparingTo("116063.00");
        }

        @Test
        void at500000_2024() {
            // ESt = 0.45 * 500000 - 18936.88 = 225000 - 18936.88 = 206063.12 → 206063
            BigDecimal tax = calculator.computeProgressiveTax(params2024, new BigDecimal("500000"));
            assertThat(tax).isEqualByComparingTo("206063.00");
        }

        @Test
        void at1000000_2024() {
            // ESt = 0.45 * 1000000 - 18936.88 = 450000 - 18936.88 = 431063.12 → 431063
            BigDecimal tax = calculator.computeProgressiveTax(params2024, new BigDecimal("1000000"));
            assertThat(tax).isEqualByComparingTo("431063.00");
        }
    }

    // ── Solidaritaetszuschlag ───────────────────────────────────────────

    @Nested
    class Solidaritaetszuschlag {

        @Test
        void noSoliWhenBelowExemption() {
            // ESt = 15,000 < 18,130 → Soli = 0
            BigDecimal soli = calculator.computeSoli(params2024, new BigDecimal("15000"));
            assertThat(soli).isEqualByComparingTo("0.00");
        }

        @Test
        void noSoliAtExactExemption() {
            BigDecimal soli = calculator.computeSoli(params2024, new BigDecimal("18130"));
            assertThat(soli).isEqualByComparingTo("0.00");
        }

        @Test
        void milderungszoneJustAboveExemption() {
            // ESt = 19,000 (just above 18,130)
            // Full Soli = 5.5% * 19000 = 1045.00
            // Milderung = 11.9% * (19000 - 18130) = 11.9% * 870 = 103.53
            // Soli = min(1045.00, 103.53) = 103.53
            BigDecimal soli = calculator.computeSoli(params2024, new BigDecimal("19000"));
            assertThat(soli).isEqualByComparingTo("103.53");
        }

        @Test
        void fullSoliForHighIncomeTax() {
            // ESt = 50,000 → well above glide zone
            // Full Soli = 5.5% * 50000 = 2750.00
            // Milderung = 11.9% * (50000 - 18130) = 11.9% * 31870 = 3792.53
            // Soli = min(2750, 3792.53) = 2750.00
            BigDecimal soli = calculator.computeSoli(params2024, new BigDecimal("50000"));
            assertThat(soli).isEqualByComparingTo("2750.00");
        }

        @Test
        void soliOnZeroTax() {
            BigDecimal soli = calculator.computeSoli(params2024, BigDecimal.ZERO);
            assertThat(soli).isEqualByComparingTo("0.00");
        }
    }

    // ── Marginal rate ───────────────────────────────────────────────────

    @Nested
    class MarginalRate {

        @Test
        void zeroForGrundfreibetrag() {
            BigDecimal rate = calculator.computeMarginalRate(params2024, new BigDecimal("11604"));
            assertThat(rate).isEqualByComparingTo("0.00");
        }

        @Test
        void about14PercentAtZone2Entry() {
            // First euro above Grundfreibetrag → ≈14%
            BigDecimal rate = calculator.computeMarginalRate(params2024, new BigDecimal("11605"));
            assertThat(rate).isBetween(new BigDecimal("13.90"), new BigDecimal("14.10"));
        }

        @Test
        void fortyTwoPercentInZone4() {
            BigDecimal rate = calculator.computeMarginalRate(params2024, new BigDecimal("100000"));
            assertThat(rate).isEqualByComparingTo("42.00");
        }

        @Test
        void fortyFivePercentInZone5() {
            BigDecimal rate = calculator.computeMarginalRate(params2024, new BigDecimal("300000"));
            assertThat(rate).isEqualByComparingTo("45.00");
        }
    }

    // ── Full calculation with deductions ────────────────────────────────

    @Nested
    class FullCalculation {

        @Test
        void typicalFreiberuflerWithExpenses() {
            // Freiberuf income: €80,000, expenses: €20,000
            // zvE = 80000 - 20000 - 36 (Sonderausgaben) = 59,964
            TaxCalculationResult result = calculator.calculate(
                    params2024,
                    BigDecimal.ZERO,
                    new BigDecimal("80000"),
                    BigDecimal.ZERO,
                    new BigDecimal("20000"),
                    BigDecimal.ZERO
            );

            assertThat(result.taxYear()).isEqualTo(2024);
            assertThat(result.totalGrossIncome()).isEqualByComparingTo("80000");
            assertThat(result.taxableIncome()).isEqualByComparingTo("59964");
            assertThat(result.incomeTax().signum()).isPositive();
            assertThat(result.effectiveRate().signum()).isPositive();
        }

        @Test
        void employmentPlusFreiberuf() {
            // Employment: €50,000, Freiberuf: €30,000, Freiberuf expenses: €10,000
            // zvE = 50000 + 30000 - 10000 - 1230 (Werbungskosten) - 36 (Sonderausgaben) = 68,734
            TaxCalculationResult result = calculator.calculate(
                    params2024,
                    new BigDecimal("50000"),
                    new BigDecimal("30000"),
                    BigDecimal.ZERO,
                    new BigDecimal("10000"),
                    BigDecimal.ZERO
            );

            assertThat(result.taxableIncome()).isEqualByComparingTo("68734");
            assertThat(result.deductions().werbungskostenpauschale())
                    .isEqualByComparingTo("1230");
            assertThat(result.deductions().sonderausgabenpauschale())
                    .isEqualByComparingTo("36");
            // Zone 4 tax (42%)
            assertThat(result.marginalRate()).isEqualByComparingTo("42.00");
        }

        @Test
        void deductionsExceedIncome_taxableIncomeZero() {
            // Income: €5,000, expenses: €10,000
            TaxCalculationResult result = calculator.calculate(
                    params2024,
                    BigDecimal.ZERO,
                    new BigDecimal("5000"),
                    BigDecimal.ZERO,
                    new BigDecimal("10000"),
                    BigDecimal.ZERO
            );

            assertThat(result.taxableIncome()).isEqualByComparingTo("0");
            assertThat(result.incomeTax()).isEqualByComparingTo("0.00");
            assertThat(result.solidaritaetszuschlag()).isEqualByComparingTo("0.00");
            assertThat(result.totalTax()).isEqualByComparingTo("0.00");
        }

        @Test
        void allThreeStreams() {
            TaxCalculationResult result = calculator.calculate(
                    params2024,
                    new BigDecimal("40000"),
                    new BigDecimal("25000"),
                    new BigDecimal("15000"),
                    new BigDecimal("5000"),
                    new BigDecimal("3000")
            );

            assertThat(result.totalGrossIncome()).isEqualByComparingTo("80000");
            // Deductions: 5000 + 3000 + 1230 + 36 = 9266
            assertThat(result.deductions().totalDeductions()).isEqualByComparingTo("9266");
            assertThat(result.taxableIncome()).isEqualByComparingTo("70734");
            assertThat(result.incomeTax().signum()).isPositive();
        }

        @Test
        void noEmployment_noWerbungskosten() {
            // Pure Freiberuf — no employment → no Werbungskostenpauschale
            TaxCalculationResult result = calculator.calculate(
                    params2024,
                    BigDecimal.ZERO,
                    new BigDecimal("50000"),
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO
            );

            assertThat(result.deductions().werbungskostenpauschale())
                    .isEqualByComparingTo("0");
            assertThat(result.deductions().sonderausgabenpauschale())
                    .isEqualByComparingTo("36");
        }
    }

    // ── 2025 year params ────────────────────────────────────────────────

    @Nested
    class Year2025 {

        @Test
        void grundfreibetrag2025() {
            BigDecimal tax = calculator.computeProgressiveTax(params2025, new BigDecimal("12084"));
            assertThat(tax).isEqualByComparingTo("0.00");

            BigDecimal taxAbove = calculator.computeProgressiveTax(params2025, new BigDecimal("12085"));
            assertThat(taxAbove).isEqualByComparingTo("0.00"); // Still truncates to 0 at 1 euro above
        }

        @Test
        void midRange2025() {
            // zvE = 50,000 for 2025
            BigDecimal tax = calculator.computeProgressiveTax(params2025, new BigDecimal("50000"));
            assertThat(tax.signum()).isPositive();
            // Should be slightly less than 2024 due to higher Grundfreibetrag
            BigDecimal tax2024 = calculator.computeProgressiveTax(params2024, new BigDecimal("50000"));
            assertThat(tax).isLessThan(tax2024);
        }
    }
}
