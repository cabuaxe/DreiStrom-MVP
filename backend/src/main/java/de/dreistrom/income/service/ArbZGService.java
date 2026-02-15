package de.dreistrom.income.service;

import de.dreistrom.income.dto.ArbZGResponse;
import de.dreistrom.socialinsurance.domain.SocialInsuranceEntry;
import de.dreistrom.socialinsurance.repository.SocialInsuranceEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Checks ArbZG §3 working time compliance.
 * Maximum allowed weekly working time is 48 hours (combined employment + self-employment).
 */
@Service
@RequiredArgsConstructor
public class ArbZGService {

    private static final BigDecimal MAX_HOURS = new BigDecimal("48.0");

    private final SocialInsuranceEntryRepository entryRepository;

    @Transactional(readOnly = true)
    public ArbZGResponse getStatus(Long userId, int year) {
        List<SocialInsuranceEntry> entries = entryRepository
                .findByUserIdAndYearOrderByMonthAsc(userId, (short) year);

        if (entries.isEmpty()) {
            return new ArbZGResponse(year, BigDecimal.ZERO, MAX_HOURS, false,
                    BigDecimal.ZERO, BigDecimal.ZERO);
        }

        BigDecimal totalEmploymentHours = BigDecimal.ZERO;
        BigDecimal totalSelfEmployedHours = BigDecimal.ZERO;

        for (SocialInsuranceEntry e : entries) {
            totalEmploymentHours = totalEmploymentHours.add(e.getEmploymentHoursWeekly());
            totalSelfEmployedHours = totalSelfEmployedHours.add(e.getSelfEmployedHoursWeekly());
        }

        BigDecimal divisor = new BigDecimal(entries.size());
        BigDecimal avgEmployment = totalEmploymentHours.divide(divisor, 1, RoundingMode.HALF_UP);
        BigDecimal avgSelfEmployed = totalSelfEmployedHours.divide(divisor, 1, RoundingMode.HALF_UP);
        BigDecimal avgTotal = avgEmployment.add(avgSelfEmployed);

        return new ArbZGResponse(
                year,
                avgTotal,
                MAX_HOURS,
                avgTotal.compareTo(MAX_HOURS) > 0,
                avgEmployment,
                avgSelfEmployed
        );
    }
}
