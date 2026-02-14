package de.dreistrom.income.listener;

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

@Slf4j
@Component
@RequiredArgsConstructor
public class AbfaerbungListener {

    private static final BigDecimal RATIO_THRESHOLD = new BigDecimal("0.03");
    private static final BigDecimal AMOUNT_THRESHOLD = new BigDecimal("24500");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final IncomeEntryRepository incomeEntryRepository;
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
        if (entry == null) {
            return;
        }

        Long userId = entry.getUser().getId();
        int year = entry.getEntryDate().getYear();
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        Long gewerbeCents = incomeEntryRepository.sumCentsByStreamAndDateRange(
                userId, "GEWERBE", yearStart, yearEnd);
        Long totalCents = incomeEntryRepository.sumCentsSelfEmployedByDateRange(
                userId, yearStart, yearEnd);

        if (gewerbeCents == null || gewerbeCents == 0L
                || totalCents == null || totalCents == 0L) {
            return;
        }

        BigDecimal gewerbe = new BigDecimal(gewerbeCents)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal total = new BigDecimal(totalCents)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal ratio = gewerbe.divide(total, 4, RoundingMode.HALF_UP);

        if (ratio.compareTo(RATIO_THRESHOLD) > 0
                && gewerbe.compareTo(AMOUNT_THRESHOLD) > 0) {
            log.warn("Abfaerbung threshold exceeded: ratio={}, gewerbeRevenue={} EUR, userId={}, year={}",
                    ratio, gewerbe, userId, year);
            eventPublisher.publishEvent(
                    new ThresholdAlert(ThresholdType.ABFAERBUNG, ratio, gewerbe, userId, year));
        }
    }
}
