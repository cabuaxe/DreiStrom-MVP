package de.dreistrom.tax.service;

import de.dreistrom.expense.repository.ExpenseEntryRepository;
import de.dreistrom.income.repository.IncomeEntryRepository;
import de.dreistrom.tax.dto.TaxReserveRecommendation;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;

/**
 * Calculates recommended monthly transfer to a tax reserve account.
 * <p>
 * Self-employed individuals should set aside 25-35% of net profit
 * (after business expenses) to cover income tax, Soli, and Gewerbesteuer.
 * The rate is configurable and adjusts for how many months remain in the year.
 */
@Service
@RequiredArgsConstructor
public class TaxReserveService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal TWELVE = new BigDecimal("12");

    private final IncomeEntryRepository incomeEntryRepository;
    private final ExpenseEntryRepository expenseEntryRepository;
    private final Clock clock;

    @Value("${dreistrom.tax.reserve.rate:30}")
    private BigDecimal reserveRatePercent;

    /**
     * Calculate recommended tax reserve for a user and year.
     *
     * @param userId          the user
     * @param year            the tax year
     * @param alreadyReserved amount already set aside this year
     * @return reserve recommendation with monthly transfer amount
     */
    @Transactional(readOnly = true)
    public TaxReserveRecommendation calculate(Long userId, int year,
                                               BigDecimal alreadyReserved) {
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        // Sum self-employed income (Freiberuf + Gewerbe)
        BigDecimal selfEmployedIncome = centsToEuros(
                incomeEntryRepository.sumCentsSelfEmployedByDateRange(
                        userId, yearStart, yearEnd));

        // Sum allocated business expenses (Freiberuf + Gewerbe)
        BigDecimal freiberufExpenses = centsToEuros(
                expenseEntryRepository.sumCentsFreiberufByDateRange(userId, yearStart, yearEnd));
        BigDecimal gewerbeExpenses = centsToEuros(
                expenseEntryRepository.sumCentsGewerbeByDateRange(userId, yearStart, yearEnd));
        BigDecimal businessExpenses = freiberufExpenses.add(gewerbeExpenses);

        BigDecimal netProfit = selfEmployedIncome.subtract(businessExpenses).max(BigDecimal.ZERO);

        // Project annual profit from year-to-date
        LocalDate today = LocalDate.now(clock);
        BigDecimal projectedAnnualProfit = projectAnnual(netProfit, year, today);

        // Annual reserve = projected profit × rate%
        BigDecimal annualReserve = projectedAnnualProfit
                .multiply(reserveRatePercent)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);

        BigDecimal remaining = annualReserve.subtract(alreadyReserved).max(BigDecimal.ZERO);

        int monthsRemaining = computeMonthsRemaining(year, today);

        BigDecimal monthlyReserve = monthsRemaining > 0
                ? remaining.divide(new BigDecimal(monthsRemaining), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new TaxReserveRecommendation(
                year, netProfit, reserveRatePercent,
                monthlyReserve, annualReserve,
                alreadyReserved, remaining, monthsRemaining);
    }

    /**
     * Pure computation for custom rate (useful for "what-if" projections).
     */
    public TaxReserveRecommendation computeWithRate(BigDecimal netProfit,
                                                     BigDecimal customRate,
                                                     BigDecimal alreadyReserved,
                                                     int year) {
        LocalDate today = LocalDate.now(clock);
        BigDecimal projected = projectAnnual(netProfit, year, today);
        BigDecimal annualReserve = projected.multiply(customRate)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal remaining = annualReserve.subtract(alreadyReserved).max(BigDecimal.ZERO);
        int monthsRemaining = computeMonthsRemaining(year, today);
        BigDecimal monthly = monthsRemaining > 0
                ? remaining.divide(new BigDecimal(monthsRemaining), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new TaxReserveRecommendation(
                year, netProfit, customRate,
                monthly, annualReserve,
                alreadyReserved, remaining, monthsRemaining);
    }

    private BigDecimal projectAnnual(BigDecimal ytdProfit, int year, LocalDate today) {
        if (today.getYear() == year && today.getMonthValue() < 12) {
            int dayOfYear = today.getDayOfYear();
            int daysInYear = today.isLeapYear() ? 366 : 365;
            if (dayOfYear > 0) {
                return ytdProfit
                        .multiply(new BigDecimal(daysInYear))
                        .divide(new BigDecimal(dayOfYear), 2, RoundingMode.HALF_UP);
            }
        }
        return ytdProfit;
    }

    private int computeMonthsRemaining(int year, LocalDate today) {
        if (today.getYear() > year) {
            return 0;
        }
        if (today.getYear() < year) {
            return 12;
        }
        return 12 - today.getMonthValue() + 1;
    }

    private BigDecimal centsToEuros(Long cents) {
        if (cents == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(cents).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }
}
