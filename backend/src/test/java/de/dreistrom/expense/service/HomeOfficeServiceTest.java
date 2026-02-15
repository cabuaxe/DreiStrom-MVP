package de.dreistrom.expense.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HomeOfficeServiceTest {

    private HomeOfficeService homeOfficeService;

    @BeforeEach
    void setUp() {
        homeOfficeService = new HomeOfficeService();
    }

    // -- Arbeitszimmer method --

    @Test
    void arbeitszimmer_calculatesProportionalDeduction() {
        // 80 m² apartment, 15 m² office = 18.75%
        // Rent 1000 + Utilities 200 = 1200/month
        // 1200 * 18.75% = 225/month * 12 months = 2700 EUR
        HomeOfficeResult result = homeOfficeService.calculateArbeitszimmer(
                new BigDecimal("1000.00"), new BigDecimal("200.00"),
                new BigDecimal("80"), new BigDecimal("15"),
                12);

        assertThat(result.method()).isEqualTo(HomeOfficeMethod.ARBEITSZIMMER);
        assertThat(result.deduction()).isEqualByComparingTo("2700.00");
        assertThat(result.details()).contains("Arbeitszimmer");
    }

    @Test
    void arbeitszimmer_partialYear_6months() {
        // 80 m² apartment, 16 m² office = 20%
        // 800 + 200 = 1000/month * 20% = 200/month * 6 months = 1200
        HomeOfficeResult result = homeOfficeService.calculateArbeitszimmer(
                new BigDecimal("800.00"), new BigDecimal("200.00"),
                new BigDecimal("80"), new BigDecimal("16"),
                6);

        assertThat(result.deduction()).isEqualByComparingTo("1200.00");
    }

    @Test
    void arbeitszimmer_smallRoom_lowDeduction() {
        // 100 m² apartment, 8 m² office = 8%
        // 700 + 150 = 850/month * 8% = 68/month * 12 = 816
        HomeOfficeResult result = homeOfficeService.calculateArbeitszimmer(
                new BigDecimal("700.00"), new BigDecimal("150.00"),
                new BigDecimal("100"), new BigDecimal("8"),
                12);

        assertThat(result.deduction()).isEqualByComparingTo("816.00");
    }

    @Test
    void arbeitszimmer_zeroUtilities() {
        // Just rent, no utilities
        // 60 m², 12 m² office = 20%, rent 600, util 0
        // 600 * 20% = 120/month * 12 = 1440
        HomeOfficeResult result = homeOfficeService.calculateArbeitszimmer(
                new BigDecimal("600.00"), BigDecimal.ZERO,
                new BigDecimal("60"), new BigDecimal("12"),
                12);

        assertThat(result.deduction()).isEqualByComparingTo("1440.00");
    }

    @Test
    void arbeitszimmer_zeroTotalArea_throwsException() {
        assertThatThrownBy(() -> homeOfficeService.calculateArbeitszimmer(
                new BigDecimal("1000.00"), new BigDecimal("200.00"),
                BigDecimal.ZERO, new BigDecimal("15"), 12))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Total area must be positive");
    }

    @Test
    void arbeitszimmer_officeExceedsTotalArea_throwsException() {
        assertThatThrownBy(() -> homeOfficeService.calculateArbeitszimmer(
                new BigDecimal("1000.00"), new BigDecimal("200.00"),
                new BigDecimal("50"), new BigDecimal("60"), 12))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Office area cannot exceed total area");
    }

    @Test
    void arbeitszimmer_invalidMonths_throwsException() {
        assertThatThrownBy(() -> homeOfficeService.calculateArbeitszimmer(
                new BigDecimal("1000.00"), new BigDecimal("200.00"),
                new BigDecimal("80"), new BigDecimal("15"), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Months must be between 1 and 12");

        assertThatThrownBy(() -> homeOfficeService.calculateArbeitszimmer(
                new BigDecimal("1000.00"), new BigDecimal("200.00"),
                new BigDecimal("80"), new BigDecimal("15"), 13))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Months must be between 1 and 12");
    }

    // -- Pauschale method --

    @Test
    void pauschale_standardDays() {
        // 150 days * 6 EUR = 900 EUR
        HomeOfficeResult result = homeOfficeService.calculatePauschale(150);

        assertThat(result.method()).isEqualTo(HomeOfficeMethod.PAUSCHALE);
        assertThat(result.deduction()).isEqualByComparingTo("900.00");
        assertThat(result.details()).contains("150 days");
    }

    @Test
    void pauschale_maxDays_cappedAt1260() {
        // 210 days * 6 = 1260 (exactly at cap)
        HomeOfficeResult result = homeOfficeService.calculatePauschale(210);
        assertThat(result.deduction()).isEqualByComparingTo("1260.00");
    }

    @Test
    void pauschale_exceedsMaxDays_cappedAt1260() {
        // 250 days > 210 max → capped at 1260
        HomeOfficeResult result = homeOfficeService.calculatePauschale(250);
        assertThat(result.deduction()).isEqualByComparingTo("1260.00");
        assertThat(result.details()).contains("capped at 210");
    }

    @Test
    void pauschale_zeroDays_returnsZero() {
        HomeOfficeResult result = homeOfficeService.calculatePauschale(0);
        assertThat(result.deduction()).isEqualByComparingTo("0.00");
    }

    @Test
    void pauschale_oneDayOnly() {
        HomeOfficeResult result = homeOfficeService.calculatePauschale(1);
        assertThat(result.deduction()).isEqualByComparingTo("6.00");
    }

    @Test
    void pauschale_negativeDays_throwsException() {
        assertThatThrownBy(() -> homeOfficeService.calculatePauschale(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be negative");
    }

    // -- Recommendation logic --

    @Test
    void recommend_arbeitszimmerBetter_whenHighRent() {
        // Arbeitszimmer: 80m², 15m² office (18.75%), 1200 EUR/month * 12 = 2700 EUR
        // Pauschale: 200 days * 6 = 1200 EUR
        // → Arbeitszimmer is better
        HomeOfficeRecommendation rec = homeOfficeService.recommend(
                new BigDecimal("1000.00"), new BigDecimal("200.00"),
                new BigDecimal("80"), new BigDecimal("15"),
                12, 200);

        assertThat(rec.recommended()).isEqualTo(HomeOfficeMethod.ARBEITSZIMMER);
        assertThat(rec.arbeitszimmer().deduction()).isEqualByComparingTo("2700.00");
        assertThat(rec.pauschale().deduction()).isEqualByComparingTo("1200.00");
    }

    @Test
    void recommend_pauschaleBetter_whenSmallRoom() {
        // Arbeitszimmer: 100m², 5m² office (5%), 600 EUR/month * 12 = 360 EUR
        // Pauschale: 200 days * 6 = 1200 EUR
        // → Pauschale is better
        HomeOfficeRecommendation rec = homeOfficeService.recommend(
                new BigDecimal("500.00"), new BigDecimal("100.00"),
                new BigDecimal("100"), new BigDecimal("5"),
                12, 200);

        assertThat(rec.recommended()).isEqualTo(HomeOfficeMethod.PAUSCHALE);
        assertThat(rec.arbeitszimmer().deduction()).isEqualByComparingTo("360.00");
        assertThat(rec.pauschale().deduction()).isEqualByComparingTo("1200.00");
    }

    @Test
    void recommend_equalAmounts_prefersArbeitszimmer() {
        // When both are equal, Arbeitszimmer is recommended (>= comparison)
        // Pauschale: 100 days * 6 = 600
        // Arbeitszimmer: need to produce exactly 600
        // 50m² total, 10m² office (20%), 250 rent + 0 util = 250/month * 20% = 50/month * 12 = 600
        HomeOfficeRecommendation rec = homeOfficeService.recommend(
                new BigDecimal("250.00"), BigDecimal.ZERO,
                new BigDecimal("50"), new BigDecimal("10"),
                12, 100);

        assertThat(rec.arbeitszimmer().deduction()).isEqualByComparingTo("600.00");
        assertThat(rec.pauschale().deduction()).isEqualByComparingTo("600.00");
        assertThat(rec.recommended()).isEqualTo(HomeOfficeMethod.ARBEITSZIMMER);
    }

    @Test
    void recommend_partialYear_arbeitszimmerWithFewMonths() {
        // Arbeitszimmer only 3 months: 80m², 20m² (25%), 1000+200=1200 * 25% = 300/month * 3 = 900
        // Pauschale: 60 days * 6 = 360
        // → Arbeitszimmer still better
        HomeOfficeRecommendation rec = homeOfficeService.recommend(
                new BigDecimal("1000.00"), new BigDecimal("200.00"),
                new BigDecimal("80"), new BigDecimal("20"),
                3, 60);

        assertThat(rec.recommended()).isEqualTo(HomeOfficeMethod.ARBEITSZIMMER);
        assertThat(rec.arbeitszimmer().deduction()).isEqualByComparingTo("900.00");
    }

    @Test
    void recommend_containsBothResults() {
        HomeOfficeRecommendation rec = homeOfficeService.recommend(
                new BigDecimal("800.00"), new BigDecimal("150.00"),
                new BigDecimal("70"), new BigDecimal("14"),
                12, 180);

        assertThat(rec.arbeitszimmer()).isNotNull();
        assertThat(rec.pauschale()).isNotNull();
        assertThat(rec.arbeitszimmer().method()).isEqualTo(HomeOfficeMethod.ARBEITSZIMMER);
        assertThat(rec.pauschale().method()).isEqualTo(HomeOfficeMethod.PAUSCHALE);
    }
}
