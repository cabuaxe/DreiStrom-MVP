package de.dreistrom.vat.service;

import de.dreistrom.income.domain.IncomeEntry;
import de.dreistrom.income.event.IncomeEntryCreated;
import de.dreistrom.income.event.IncomeEntryModified;
import de.dreistrom.income.event.ThresholdAlert;
import de.dreistrom.income.event.ThresholdType;
import de.dreistrom.income.repository.IncomeEntryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class KleinunternehmerMonitor {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final IncomeEntryRepository incomeEntryRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Value("${dreistrom.vat.kleinunternehmer.current-year-limit:22000}")
    private BigDecimal currentYearLimit;

    @Value("${dreistrom.vat.kleinunternehmer.projected-year-limit:50000}")
    private BigDecimal projectedYearLimit;

    @Value("${dreistrom.vat.kleinunternehmer.warning-ratio:0.80}")
    private BigDecimal warningRatio;

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

        Long totalCents = incomeEntryRepository.sumCentsSelfEmployedByDateRange(
                userId, yearStart, yearEnd);
        if (totalCents == null || totalCents == 0L) {
            return;
        }

        BigDecimal revenue = new BigDecimal(totalCents)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);

        checkCurrentYear(revenue, userId, year);
        checkProjected(revenue, userId, year);
    }

    private void checkCurrentYear(BigDecimal revenue, Long userId, int year) {
        BigDecimal ratio = revenue.divide(currentYearLimit, 4, RoundingMode.HALF_UP);
        if (ratio.compareTo(warningRatio) >= 0) {
            log.warn("§19 UStG current-year threshold: ratio={}, revenue={} EUR, limit={} EUR, userId={}, year={}",
                    ratio, revenue, currentYearLimit, userId, year);
            eventPublisher.publishEvent(
                    new ThresholdAlert(ThresholdType.KLEINUNTERNEHMER_CURRENT_YEAR,
                            ratio, revenue, userId, year));
        }
    }

    private void checkProjected(BigDecimal revenue, Long userId, int year) {
        LocalDate today = LocalDate.now(clock);

        int dayOfYear;
        int daysInYear;

        if (year == today.getYear()) {
            dayOfYear = today.getDayOfYear();
            daysInYear = today.isLeapYear() ? 366 : 365;
        } else if (year < today.getYear()) {
            // Past year is complete — actual total equals projection
            dayOfYear = yearEnd(year).getDayOfYear();
            daysInYear = dayOfYear;
        } else {
            // Future year — skip projection
            return;
        }

        if (dayOfYear == 0) {
            return;
        }

        BigDecimal projected = revenue
                .multiply(new BigDecimal(daysInYear))
                .divide(new BigDecimal(dayOfYear), 2, RoundingMode.HALF_UP);

        BigDecimal ratio = projected.divide(projectedYearLimit, 4, RoundingMode.HALF_UP);
        if (ratio.compareTo(warningRatio) >= 0) {
            log.warn("§19 UStG projected threshold: ratio={}, projected={} EUR, limit={} EUR, userId={}, year={}",
                    ratio, projected, projectedYearLimit, userId, year);
            eventPublisher.publishEvent(
                    new ThresholdAlert(ThresholdType.KLEINUNTERNEHMER_PROJECTED,
                            ratio, projected, userId, year));
        }
    }

    private static LocalDate yearEnd(int year) {
        return LocalDate.of(year, 12, 31);
    }
}
