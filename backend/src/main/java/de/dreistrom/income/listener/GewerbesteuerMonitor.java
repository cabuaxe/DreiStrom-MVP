package de.dreistrom.income.listener;

import de.dreistrom.common.domain.IncomeStream;
import de.dreistrom.expense.repository.ExpenseEntryRepository;
import de.dreistrom.income.domain.IncomeEntry;
import de.dreistrom.income.event.IncomeEntryCreated;
import de.dreistrom.income.event.IncomeEntryModified;
import de.dreistrom.income.event.ThresholdAlert;
import de.dreistrom.income.event.ThresholdType;
import de.dreistrom.income.repository.IncomeEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Monitors Gewerbesteuer thresholds on income changes:
 * <ul>
 *   <li>Freibetrag: profit > €24,500 → Gewerbesteuer due</li>
 *   <li>Bilanzierungspflicht §141 AO: revenue > €800,000 OR profit > €80,000</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GewerbesteuerMonitor {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal FREIBETRAG = new BigDecimal("24500");
    private static final BigDecimal BILANZIERUNG_REVENUE = new BigDecimal("800000");
    private static final BigDecimal BILANZIERUNG_PROFIT = new BigDecimal("80000");

    private final IncomeEntryRepository incomeEntryRepository;
    private final ExpenseEntryRepository expenseEntryRepository;
    private final ApplicationEventPublisher eventPublisher;

    @EventListener
    public void onIncomeCreated(IncomeEntryCreated event) {
        evaluate(event.getAggregateId());
    }

    @EventListener
    public void onIncomeModified(IncomeEntryModified event) {
        evaluate(event.getAggregateId());
    }

    private void evaluate(Long entryId) {
        IncomeEntry entry = incomeEntryRepository.findById(entryId).orElse(null);
        if (entry == null || entry.getStreamType() != IncomeStream.GEWERBE) {
            return;
        }

        Long userId = entry.getUser().getId();
        int year = entry.getEntryDate().getYear();
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        BigDecimal gewerbeRevenue = centsToEuros(
                incomeEntryRepository.sumCentsByStreamAndDateRange(
                        userId, "GEWERBE", yearStart, yearEnd));
        BigDecimal gewerbeExpenses = centsToEuros(
                expenseEntryRepository.sumCentsGewerbeByDateRange(userId, yearStart, yearEnd));
        BigDecimal profit = gewerbeRevenue.subtract(gewerbeExpenses).max(BigDecimal.ZERO);

        if (profit.compareTo(FREIBETRAG) > 0) {
            BigDecimal ratio = profit.divide(FREIBETRAG, 4, RoundingMode.HALF_UP);
            log.warn("GewSt Freibetrag exceeded: profit={} EUR, userId={}, year={}",
                    profit, userId, year);
            eventPublisher.publishEvent(
                    new ThresholdAlert(ThresholdType.GEWERBESTEUER_FREIBETRAG,
                            ratio, gewerbeRevenue, userId, year));
        }

        if (gewerbeRevenue.compareTo(BILANZIERUNG_REVENUE) > 0
                || profit.compareTo(BILANZIERUNG_PROFIT) > 0) {
            log.warn("Bilanzierungspflicht triggered: revenue={} EUR, profit={} EUR, userId={}, year={}",
                    gewerbeRevenue, profit, userId, year);
            eventPublisher.publishEvent(
                    new ThresholdAlert(ThresholdType.BILANZIERUNG,
                            BigDecimal.ONE, gewerbeRevenue, userId, year));
        }
    }

    private BigDecimal centsToEuros(Long cents) {
        if (cents == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(cents).divide(HUNDRED, 2, RoundingMode.HALF_UP);
    }
}
