package de.dreistrom.tax.service;

import de.dreistrom.tax.dto.GewerbesteuerResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class GewerbesteuerCalculatorTest {

    private GewerbesteuerCalculator calculator;

    @BeforeEach
    void setUp() {
        // Use constructor with null repos since we only test compute()
        calculator = new GewerbesteuerCalculator(null, null);
        ReflectionTestUtils.setField(calculator, "hebesatz", 410);
    }

    // ── Freibetrag ──────────────────────────────────────────────────────

    @Nested
    class Freibetrag {

        @Test
        void profitBelowFreibetrag_zeroTax() {
            GewerbesteuerResult result = calculator.compute(
                    new BigDecimal("20000"), BigDecimal.ZERO, new BigDecimal("5000"));

            assertThat(result.gewerbeProfit()).isEqualByComparingTo("20000");
            assertThat(result.taxableProfit()).isEqualByComparingTo("0");
            assertThat(result.gewerbesteuer()).isEqualByComparingTo("0.00");
            assertThat(result.netGewerbesteuer()).isEqualByComparingTo("0.00");
        }

        @Test
        void profitExactlyFreibetrag_zeroTax() {
            GewerbesteuerResult result = calculator.compute(
                    new BigDecimal("24500"), BigDecimal.ZERO, new BigDecimal("5000"));

            assertThat(result.taxableProfit()).isEqualByComparingTo("0");
            assertThat(result.gewerbesteuer()).isEqualByComparingTo("0.00");
        }

        @Test
        void negativeProfit_zeroTax() {
            GewerbesteuerResult result = calculator.compute(
                    new BigDecimal("10000"), new BigDecimal("15000"), new BigDecimal("5000"));

            assertThat(result.gewerbeProfit()).isEqualByComparingTo("0");
            assertThat(result.taxableProfit()).isEqualByComparingTo("0");
            assertThat(result.gewerbesteuer()).isEqualByComparingTo("0.00");
        }
    }

    // ── Standard computation ────────────────────────────────────────────

    @Nested
    class StandardComputation {

        @Test
        void typicalGewerbeProfit_berlinHebesatz() {
            // Profit = €60,000, Freibetrag = €24,500 → Taxable = €35,500
            // Messbetrag = 35500 * 3.5% = 1242.50
            // GewSt = 1242.50 * 410 / 100 = 5094.25
            GewerbesteuerResult result = calculator.compute(
                    new BigDecimal("60000"), BigDecimal.ZERO, new BigDecimal("20000"));

            assertThat(result.gewerbeProfit()).isEqualByComparingTo("60000");
            assertThat(result.taxableProfit()).isEqualByComparingTo("35500");
            assertThat(result.steuermessbetrag()).isEqualByComparingTo("1242.50");
            assertThat(result.gewerbesteuer()).isEqualByComparingTo("5094.25");
        }

        @Test
        void withBusinessExpenses() {
            // Income €80,000, Expenses €15,000 → Profit = €65,000
            // Taxable = 65000 - 24500 = 40500
            // Messbetrag = 40500 * 0.035 = 1417.50
            // GewSt = 1417.50 * 410/100 = 5811.75
            GewerbesteuerResult result = calculator.compute(
                    new BigDecimal("80000"), new BigDecimal("15000"), new BigDecimal("25000"));

            assertThat(result.gewerbeProfit()).isEqualByComparingTo("65000");
            assertThat(result.taxableProfit()).isEqualByComparingTo("40500");
            assertThat(result.steuermessbetrag()).isEqualByComparingTo("1417.50");
            assertThat(result.gewerbesteuer()).isEqualByComparingTo("5811.75");
        }

        @Test
        void highProfit() {
            // Profit = €200,000 → Taxable = 175,500
            // Messbetrag = 175500 * 0.035 = 6142.50
            // GewSt = 6142.50 * 410/100 = 25184.25
            GewerbesteuerResult result = calculator.compute(
                    new BigDecimal("200000"), BigDecimal.ZERO, new BigDecimal("70000"));

            assertThat(result.taxableProfit()).isEqualByComparingTo("175500");
            assertThat(result.steuermessbetrag()).isEqualByComparingTo("6142.50");
            assertThat(result.gewerbesteuer()).isEqualByComparingTo("25184.25");
        }
    }

    // ── §35 EStG credit ─────────────────────────────────────────────────

    @Nested
    class Paragraph35Credit {

        @Test
        void creditIs4xMessbetrag_whenSufficientIncomeTax() {
            // Profit = €60,000 → Messbetrag = 1242.50
            // 4.0 × 1242.50 = 4970.00
            // IncomeTax = 20,000 > 4970 → credit = 4970.00
            GewerbesteuerResult result = calculator.compute(
                    new BigDecimal("60000"), BigDecimal.ZERO, new BigDecimal("20000"));

            assertThat(result.paragraph35Credit()).isEqualByComparingTo("4970.00");
            // Net = 5094.25 - 4970.00 = 124.25
            assertThat(result.netGewerbesteuer()).isEqualByComparingTo("124.25");
        }

        @Test
        void creditCappedAtIncomeTax() {
            // Profit = €60,000 → Messbetrag = 1242.50
            // 4.0 × 1242.50 = 4970.00
            // IncomeTax = 3000 < 4970 → credit = 3000.00
            GewerbesteuerResult result = calculator.compute(
                    new BigDecimal("60000"), BigDecimal.ZERO, new BigDecimal("3000"));

            assertThat(result.paragraph35Credit()).isEqualByComparingTo("3000.00");
            // Net = 5094.25 - 3000.00 = 2094.25
            assertThat(result.netGewerbesteuer()).isEqualByComparingTo("2094.25");
        }

        @Test
        void creditZeroWhenZeroIncomeTax() {
            GewerbesteuerResult result = calculator.compute(
                    new BigDecimal("60000"), BigDecimal.ZERO, BigDecimal.ZERO);

            assertThat(result.paragraph35Credit()).isEqualByComparingTo("0.00");
            assertThat(result.netGewerbesteuer()).isEqualByComparingTo("5094.25");
        }

        @Test
        void creditDoesNotExceedGewerbesteuer() {
            // Even though 4× Messbetrag could exceed GewSt at low Hebesatz,
            // with Berlin 410% (> 400%), net is always slightly positive.
            // Test net >= 0
            GewerbesteuerResult result = calculator.compute(
                    new BigDecimal("60000"), BigDecimal.ZERO, new BigDecimal("100000"));

            assertThat(result.netGewerbesteuer().signum()).isGreaterThanOrEqualTo(0);
        }
    }

    // ── Hebesatz variations ─────────────────────────────────────────────

    @Nested
    class HebesatzVariations {

        @Test
        void munich_490() {
            ReflectionTestUtils.setField(calculator, "hebesatz", 490);
            // Profit = €60,000 → Taxable = 35500, Messbetrag = 1242.50
            // GewSt = 1242.50 * 490/100 = 6088.25
            GewerbesteuerResult result = calculator.compute(
                    new BigDecimal("60000"), BigDecimal.ZERO, new BigDecimal("20000"));

            assertThat(result.hebesatz()).isEqualTo(490);
            assertThat(result.gewerbesteuer()).isEqualByComparingTo("6088.25");
        }

        @Test
        void minimum_200() {
            ReflectionTestUtils.setField(calculator, "hebesatz", 200);
            // At 200% Hebesatz, §35 credit (4×Messbetrag) fully covers GewSt
            // GewSt = 1242.50 * 200/100 = 2485.00
            // Credit = min(4970, 20000) = 4970 → but net capped at 0
            GewerbesteuerResult result = calculator.compute(
                    new BigDecimal("60000"), BigDecimal.ZERO, new BigDecimal("20000"));

            assertThat(result.gewerbesteuer()).isEqualByComparingTo("2485.00");
            assertThat(result.netGewerbesteuer()).isEqualByComparingTo("0.00");
        }
    }

    // ── Zero Gewerbe data ───────────────────────────────────────────────

    @Nested
    class ZeroGewerbeData {

        @Test
        void zeroIncome_zeroResult() {
            GewerbesteuerResult result = calculator.compute(
                    BigDecimal.ZERO, BigDecimal.ZERO, new BigDecimal("10000"));

            assertThat(result.gewerbeProfit()).isEqualByComparingTo("0");
            assertThat(result.gewerbesteuer()).isEqualByComparingTo("0.00");
            assertThat(result.paragraph35Credit()).isEqualByComparingTo("0.00");
            assertThat(result.netGewerbesteuer()).isEqualByComparingTo("0.00");
        }
    }
}
