package de.dreistrom.tax.service;

import de.dreistrom.expense.repository.ExpenseEntryRepository;
import de.dreistrom.income.repository.IncomeEntryRepository;
import de.dreistrom.tax.domain.TaxYearParams;
import de.dreistrom.tax.dto.TaxCalculationResult;
import de.dreistrom.tax.event.TaxCalculated;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Orchestrates income tax assessment by aggregating income and expense data
 * from repositories and delegating to {@link IncomeTaxCalculator} for the
 * §32a EStG progressive computation.
 */
@Service
@RequiredArgsConstructor
public class TaxAssessmentService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final IncomeEntryRepository incomeEntryRepository;
    private final ExpenseEntryRepository expenseEntryRepository;
    private final IncomeTaxCalculator incomeTaxCalculator;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Compute projected income tax for a user and tax year.
     * Aggregates all income entries and allocated business expenses,
     * then applies the §32a progressive schedule.
     *
     * @param userId the user
     * @param year   the tax year
     * @return full tax calculation result
     */
    @Transactional(readOnly = true)
    public TaxCalculationResult assess(Long userId, int year) {
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        TaxYearParams params = TaxYearParams.forYear(year);

        // Aggregate income by stream
        BigDecimal employmentIncome = centsToEuros(
                incomeEntryRepository.sumCentsByStreamAndDateRange(
                        userId, "EMPLOYMENT", yearStart, yearEnd));
        BigDecimal freiberufIncome = centsToEuros(
                incomeEntryRepository.sumCentsByStreamAndDateRange(
                        userId, "FREIBERUF", yearStart, yearEnd));
        BigDecimal gewerbeIncome = centsToEuros(
                incomeEntryRepository.sumCentsByStreamAndDateRange(
                        userId, "GEWERBE", yearStart, yearEnd));

        // Aggregate allocated business expenses by stream
        BigDecimal freiberufExpenses = centsToEuros(
                expenseEntryRepository.sumCentsFreiberufByDateRange(userId, yearStart, yearEnd));
        BigDecimal gewerbeExpenses = centsToEuros(
                expenseEntryRepository.sumCentsGewerbeByDateRange(userId, yearStart, yearEnd));

        TaxCalculationResult result = incomeTaxCalculator.calculate(
                params,
                employmentIncome, freiberufIncome, gewerbeIncome,
                freiberufExpenses, gewerbeExpenses);

        eventPublisher.publishEvent(new TaxCalculated(
                userId, year, result.totalTax(), result.effectiveRate()));

        return result;
    }

    private BigDecimal centsToEuros(Long cents) {
        if (cents == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(cents).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }
}
