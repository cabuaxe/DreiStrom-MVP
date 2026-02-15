package de.dreistrom.income.service;

import de.dreistrom.expense.repository.ExpenseEntryRepository;
import de.dreistrom.income.dto.GewerbesteuerThresholdResponse;
import de.dreistrom.income.repository.IncomeEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Computes Gewerbesteuer threshold status for the dashboard:
 * <ul>
 *   <li>GewSt Freibetrag: €24,500</li>
 *   <li>Bilanzierungspflicht §141 AO: Revenue > €800,000 OR Profit > €80,000</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class GewerbesteuerThresholdService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal FREIBETRAG = new BigDecimal("24500");
    private static final BigDecimal BILANZIERUNG_REVENUE = new BigDecimal("800000");
    private static final BigDecimal BILANZIERUNG_PROFIT = new BigDecimal("80000");

    private final IncomeEntryRepository incomeEntryRepository;
    private final ExpenseEntryRepository expenseEntryRepository;

    @Transactional(readOnly = true)
    public GewerbesteuerThresholdResponse getStatus(Long userId, int year) {
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        BigDecimal gewerbeRevenue = centsToEuros(
                incomeEntryRepository.sumCentsByStreamAndDateRange(
                        userId, "GEWERBE", yearStart, yearEnd));
        BigDecimal gewerbeExpenses = centsToEuros(
                expenseEntryRepository.sumCentsGewerbeByDateRange(userId, yearStart, yearEnd));

        BigDecimal gewerbeProfit = gewerbeRevenue.subtract(gewerbeExpenses).max(BigDecimal.ZERO);

        return new GewerbesteuerThresholdResponse(
                year,
                gewerbeProfit,
                FREIBETRAG,
                gewerbeProfit.compareTo(FREIBETRAG) > 0,
                gewerbeRevenue,
                BILANZIERUNG_REVENUE,
                gewerbeRevenue.compareTo(BILANZIERUNG_REVENUE) > 0,
                BILANZIERUNG_PROFIT,
                gewerbeProfit.compareTo(BILANZIERUNG_PROFIT) > 0
        );
    }

    private BigDecimal centsToEuros(Long cents) {
        if (cents == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(cents).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }
}
