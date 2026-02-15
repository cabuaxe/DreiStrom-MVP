package de.dreistrom.income.listener;

import de.dreistrom.common.domain.IncomeStream;
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
 * Monitors mandatory filing threshold per §46 Abs. 2 Nr. 1 EStG.
 * Filing is required when Nebeneinkünfte (FREIBERUF + GEWERBE) exceed €410.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MandatoryFilingMonitor {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal THRESHOLD = new BigDecimal("410");

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
        if (entry == null || entry.getStreamType() == IncomeStream.EMPLOYMENT) {
            return;
        }

        Long userId = entry.getUser().getId();
        int year = entry.getEntryDate().getYear();
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        Long selfEmployedCents = incomeEntryRepository.sumCentsSelfEmployedByDateRange(
                userId, yearStart, yearEnd);
        if (selfEmployedCents == null) {
            return;
        }

        BigDecimal nebeneinkuenfte = new BigDecimal(selfEmployedCents)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);

        if (nebeneinkuenfte.compareTo(THRESHOLD) > 0) {
            BigDecimal ratio = nebeneinkuenfte.divide(THRESHOLD, 4, RoundingMode.HALF_UP);
            log.info("Mandatory filing triggered: Nebeneinkünfte={} EUR > {} EUR, userId={}, year={}",
                    nebeneinkuenfte, THRESHOLD, userId, year);
            eventPublisher.publishEvent(
                    new ThresholdAlert(ThresholdType.MANDATORY_FILING,
                            ratio, nebeneinkuenfte, userId, year));
        }
    }
}
