package de.dreistrom.expense.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Home office deduction calculator per REQ-3.3.
 *
 * Supports two methods:
 * (a) Arbeitszimmer — dedicated home office room: proportional share of
 *     rent + utilities based on floor area ratio.
 * (b) Homeoffice-Pauschale — flat rate: 6 EUR/day, max 1,260 EUR/year.
 *
 * Auto-recommends the more advantageous method based on actual data.
 */
@Service
public class HomeOfficeService {

    /** Homeoffice-Pauschale: 6 EUR per day */
    private static final BigDecimal PAUSCHALE_PER_DAY = new BigDecimal("6.00");

    /** Homeoffice-Pauschale: max 1,260 EUR per year */
    private static final BigDecimal PAUSCHALE_MAX_YEAR = new BigDecimal("1260.00");

    /** Maximum days for Pauschale: 1260 / 6 = 210 days */
    private static final int PAUSCHALE_MAX_DAYS = 210;

    /**
     * Calculate deduction using the Arbeitszimmer method.
     *
     * The deductible amount is the proportional share of total housing costs
     * (rent + utilities) based on the ratio of office floor area to total
     * living area.
     *
     * @param monthlyRent       monthly rent in EUR
     * @param monthlyUtilities  monthly utilities (Nebenkosten) in EUR
     * @param totalAreaSqm      total living area in square metres
     * @param officeAreaSqm     dedicated office area in square metres
     * @param months            number of months the office was used (1-12)
     * @return the calculation result
     */
    public HomeOfficeResult calculateArbeitszimmer(BigDecimal monthlyRent,
                                                    BigDecimal monthlyUtilities,
                                                    BigDecimal totalAreaSqm,
                                                    BigDecimal officeAreaSqm,
                                                    int months) {
        if (totalAreaSqm.signum() <= 0) {
            throw new IllegalArgumentException("Total area must be positive");
        }
        if (officeAreaSqm.compareTo(totalAreaSqm) > 0) {
            throw new IllegalArgumentException(
                    "Office area cannot exceed total area");
        }
        if (months < 1 || months > 12) {
            throw new IllegalArgumentException("Months must be between 1 and 12");
        }

        BigDecimal areaRatio = officeAreaSqm.divide(totalAreaSqm, 10, RoundingMode.HALF_UP);
        BigDecimal monthlyCosts = monthlyRent.add(monthlyUtilities);
        BigDecimal monthlyDeduction = monthlyCosts.multiply(areaRatio);
        BigDecimal totalDeduction = monthlyDeduction.multiply(new BigDecimal(months))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal ratioPercent = areaRatio.multiply(new BigDecimal("100"))
                .setScale(1, RoundingMode.HALF_UP);

        String details = String.format(
                "Arbeitszimmer: %.1f m² / %.1f m² = %s%% of (%.2f + %.2f) EUR/month × %d months",
                officeAreaSqm, totalAreaSqm, ratioPercent.toPlainString(),
                monthlyRent, monthlyUtilities, months);

        return new HomeOfficeResult(HomeOfficeMethod.ARBEITSZIMMER, totalDeduction, details);
    }

    /**
     * Calculate deduction using the Homeoffice-Pauschale method.
     *
     * Flat rate of 6 EUR per home office day, capped at 1,260 EUR/year
     * (= max 210 days).
     *
     * @param homeOfficeDays number of days worked from home in the year
     * @return the calculation result
     */
    public HomeOfficeResult calculatePauschale(int homeOfficeDays) {
        if (homeOfficeDays < 0) {
            throw new IllegalArgumentException("Home office days cannot be negative");
        }

        int effectiveDays = Math.min(homeOfficeDays, PAUSCHALE_MAX_DAYS);
        BigDecimal deduction = PAUSCHALE_PER_DAY.multiply(new BigDecimal(effectiveDays))
                .min(PAUSCHALE_MAX_YEAR)
                .setScale(2, RoundingMode.HALF_UP);

        String details;
        if (homeOfficeDays > PAUSCHALE_MAX_DAYS) {
            details = String.format(
                    "Homeoffice-Pauschale: %d days (capped at %d) × %.2f EUR = %.2f EUR (max %.2f)",
                    homeOfficeDays, PAUSCHALE_MAX_DAYS, PAUSCHALE_PER_DAY,
                    deduction, PAUSCHALE_MAX_YEAR);
        } else {
            details = String.format(
                    "Homeoffice-Pauschale: %d days × %.2f EUR = %.2f EUR",
                    homeOfficeDays, PAUSCHALE_PER_DAY, deduction);
        }

        return new HomeOfficeResult(HomeOfficeMethod.PAUSCHALE, deduction, details);
    }

    /**
     * Compare both methods and recommend the more advantageous one.
     *
     * @param monthlyRent       monthly rent in EUR
     * @param monthlyUtilities  monthly utilities in EUR
     * @param totalAreaSqm      total living area in square metres
     * @param officeAreaSqm     dedicated office area in square metres
     * @param months            months the office was used (1-12)
     * @param homeOfficeDays    days worked from home in the year
     * @return recommendation with both results and the better method
     */
    public HomeOfficeRecommendation recommend(BigDecimal monthlyRent,
                                               BigDecimal monthlyUtilities,
                                               BigDecimal totalAreaSqm,
                                               BigDecimal officeAreaSqm,
                                               int months,
                                               int homeOfficeDays) {
        HomeOfficeResult arbeitszimmer = calculateArbeitszimmer(
                monthlyRent, monthlyUtilities, totalAreaSqm, officeAreaSqm, months);
        HomeOfficeResult pauschale = calculatePauschale(homeOfficeDays);

        HomeOfficeMethod recommended = arbeitszimmer.deduction()
                .compareTo(pauschale.deduction()) >= 0
                ? HomeOfficeMethod.ARBEITSZIMMER
                : HomeOfficeMethod.PAUSCHALE;

        return new HomeOfficeRecommendation(arbeitszimmer, pauschale, recommended);
    }
}
