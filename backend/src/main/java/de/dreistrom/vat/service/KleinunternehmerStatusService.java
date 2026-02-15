package de.dreistrom.vat.service;

import de.dreistrom.income.repository.IncomeEntryRepository;
import de.dreistrom.vat.dto.KleinunternehmerStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;

/**
 * Provides Kleinunternehmer (§19 UStG) threshold status for the dashboard.
 * Reuses the same thresholds as KleinunternehmerMonitor.
 */
@Service
@RequiredArgsConstructor
public class KleinunternehmerStatusService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final IncomeEntryRepository incomeEntryRepository;
    private final Clock clock;

    @Value("${dreistrom.vat.kleinunternehmer.current-year-limit:22000}")
    private BigDecimal currentYearLimit;

    @Value("${dreistrom.vat.kleinunternehmer.projected-year-limit:50000}")
    private BigDecimal projectedYearLimit;

    @Transactional(readOnly = true)
    public KleinunternehmerStatus getStatus(Long userId, int year) {
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        Long totalCents = incomeEntryRepository.sumCentsSelfEmployedByDateRange(
                userId, yearStart, yearEnd);
        BigDecimal revenue = totalCents != null
                ? new BigDecimal(totalCents).divide(HUNDRED, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal currentRatio = currentYearLimit.signum() > 0
                ? revenue.divide(currentYearLimit, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        BigDecimal projected = projectAnnual(revenue, year);
        BigDecimal projectedRatio = projectedYearLimit.signum() > 0
                ? projected.divide(projectedYearLimit, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new KleinunternehmerStatus(
                year,
                revenue,
                currentYearLimit,
                currentRatio,
                projected,
                projectedYearLimit,
                projectedRatio,
                currentRatio.compareTo(BigDecimal.ONE) >= 0,
                projectedRatio.compareTo(BigDecimal.ONE) >= 0
        );
    }

    private BigDecimal projectAnnual(BigDecimal revenue, int year) {
        LocalDate today = LocalDate.now(clock);
        if (year != today.getYear() || revenue.signum() == 0) {
            return revenue;
        }
        int dayOfYear = today.getDayOfYear();
        int daysInYear = today.isLeapYear() ? 366 : 365;
        return revenue.multiply(new BigDecimal(daysInYear))
                .divide(new BigDecimal(dayOfYear), 2, RoundingMode.HALF_UP);
    }
}
