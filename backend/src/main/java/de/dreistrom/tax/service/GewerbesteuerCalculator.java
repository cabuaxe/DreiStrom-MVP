package de.dreistrom.tax.service;

import de.dreistrom.expense.repository.ExpenseEntryRepository;
import de.dreistrom.income.repository.IncomeEntryRepository;
import de.dreistrom.tax.dto.GewerbesteuerResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Computes Gewerbesteuer (trade tax) on Gewerbe profits.
 * <p>
 * Formula:
 * <ol>
 *   <li>Gewerbeertrag = Gewerbe income - Gewerbe business expenses</li>
 *   <li>Taxable = max(0, Gewerbeertrag - Freibetrag)</li>
 *   <li>Steuermessbetrag = Taxable × Steuermesszahl (3.5%)</li>
 *   <li>Gewerbesteuer = Steuermessbetrag × Hebesatz / 100</li>
 *   <li>§35 EStG credit = min(4.0 × Steuermessbetrag, actual ESt attributable to Gewerbe)</li>
 *   <li>Net burden = Gewerbesteuer - §35 credit</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class GewerbesteuerCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal FREIBETRAG = new BigDecimal("24500");
    private static final BigDecimal STEUERMESSZAHL = new BigDecimal("0.035");
    private static final BigDecimal PARAGRAPH_35_FACTOR = new BigDecimal("4.0");

    private final IncomeEntryRepository incomeEntryRepository;
    private final ExpenseEntryRepository expenseEntryRepository;

    @Value("${dreistrom.tax.gewerbesteuer.hebesatz:410}")
    private int hebesatz;

    /**
     * Compute Gewerbesteuer for a user and tax year.
     *
     * @param userId    the user
     * @param year      the tax year
     * @param incomeTax total income tax (for §35 credit cap)
     * @return Gewerbesteuer calculation result, or zero result if no Gewerbe data
     */
    @Transactional(readOnly = true)
    public GewerbesteuerResult calculate(Long userId, int year, BigDecimal incomeTax) {
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        BigDecimal gewerbeIncome = centsToEuros(
                incomeEntryRepository.sumCentsByStreamAndDateRange(
                        userId, "GEWERBE", yearStart, yearEnd));
        BigDecimal gewerbeExpenses = centsToEuros(
                expenseEntryRepository.sumCentsGewerbeByDateRange(userId, yearStart, yearEnd));

        return compute(gewerbeIncome, gewerbeExpenses, incomeTax);
    }

    /**
     * Pure computation without DB access. Useful for projections and testing.
     *
     * @param gewerbeIncome   gross Gewerbe income
     * @param gewerbeExpenses allocated Gewerbe business expenses
     * @param incomeTax       total income tax (for §35 credit cap)
     * @return Gewerbesteuer calculation result
     */
    public GewerbesteuerResult compute(BigDecimal gewerbeIncome,
                                       BigDecimal gewerbeExpenses,
                                       BigDecimal incomeTax) {
        BigDecimal profit = gewerbeIncome.subtract(gewerbeExpenses).max(BigDecimal.ZERO);
        BigDecimal taxableProfit = profit.subtract(FREIBETRAG).max(BigDecimal.ZERO);

        // Steuermessbetrag = taxable × 3.5%
        BigDecimal messbetrag = taxableProfit.multiply(STEUERMESSZAHL)
                .setScale(2, RoundingMode.HALF_UP);

        // Gewerbesteuer = Messbetrag × Hebesatz / 100
        BigDecimal gewSteuer = messbetrag.multiply(new BigDecimal(hebesatz))
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);

        // §35 EStG credit: min(4.0 × Messbetrag, income tax)
        BigDecimal maxCredit = PARAGRAPH_35_FACTOR.multiply(messbetrag)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal credit = maxCredit.min(incomeTax.max(BigDecimal.ZERO));

        // Net burden (cannot be negative — credit capped at Gewerbesteuer)
        BigDecimal netBurden = gewSteuer.subtract(credit).max(BigDecimal.ZERO);

        return new GewerbesteuerResult(
                profit,
                FREIBETRAG,
                taxableProfit,
                STEUERMESSZAHL,
                messbetrag,
                hebesatz,
                gewSteuer,
                credit,
                netBurden
        );
    }

    private BigDecimal centsToEuros(Long cents) {
        if (cents == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(cents).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }
}
