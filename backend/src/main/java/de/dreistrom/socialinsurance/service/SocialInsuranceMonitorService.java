package de.dreistrom.socialinsurance.service;

import de.dreistrom.socialinsurance.domain.SocialInsuranceEntry;
import de.dreistrom.socialinsurance.dto.SocialInsuranceStatus;
import de.dreistrom.socialinsurance.dto.SocialInsuranceStatus.MonthlyBreakdown;
import de.dreistrom.socialinsurance.dto.SocialInsuranceStatus.RiskLevel;
import de.dreistrom.socialinsurance.repository.SocialInsuranceEntryRepository;
import de.dreistrom.common.domain.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Monitors primary/secondary classification for social insurance.
 * §5 Abs. 5 SGB V: self-employment is secondary if employment
 * is the main activity (>20 hours/week AND higher income).
 * Reclassification risk arises when self-employment exceeds these thresholds.
 */
@Service
@RequiredArgsConstructor
public class SocialInsuranceMonitorService {

    private static final BigDecimal HOURS_THRESHOLD = new BigDecimal("20.0");

    private final SocialInsuranceEntryRepository entryRepository;

    /**
     * Get the social insurance status assessment for a user and year.
     */
    @Transactional(readOnly = true)
    public SocialInsuranceStatus getStatus(Long userId, int year) {
        List<SocialInsuranceEntry> entries = entryRepository
                .findByUserIdAndYearOrderByMonthAsc(userId, (short) year);

        List<MonthlyBreakdown> monthlyData = entries.stream()
                .map(e -> new MonthlyBreakdown(
                        e.getMonth(),
                        e.getEmploymentHoursWeekly(),
                        e.getSelfEmployedHoursWeekly(),
                        e.getEmploymentIncome(),
                        e.getSelfEmployedIncome()))
                .toList();

        if (entries.isEmpty()) {
            return new SocialInsuranceStatus(year,
                    BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO,
                    false, false, RiskLevel.SAFE,
                    "Keine Daten für " + year + " vorhanden.",
                    monthlyData);
        }

        BigDecimal totalEmploymentIncome = BigDecimal.ZERO;
        BigDecimal totalSelfEmployedIncome = BigDecimal.ZERO;
        BigDecimal totalEmploymentHours = BigDecimal.ZERO;
        BigDecimal totalSelfEmployedHours = BigDecimal.ZERO;

        for (SocialInsuranceEntry e : entries) {
            totalEmploymentIncome = totalEmploymentIncome.add(e.getEmploymentIncome());
            totalSelfEmployedIncome = totalSelfEmployedIncome.add(e.getSelfEmployedIncome());
            totalEmploymentHours = totalEmploymentHours.add(e.getEmploymentHoursWeekly());
            totalSelfEmployedHours = totalSelfEmployedHours.add(e.getSelfEmployedHoursWeekly());
        }

        int count = entries.size();
        BigDecimal divisor = new BigDecimal(count);
        BigDecimal avgEmploymentHours = totalEmploymentHours.divide(divisor, 1, RoundingMode.HALF_UP);
        BigDecimal avgSelfEmployedHours = totalSelfEmployedHours.divide(divisor, 1, RoundingMode.HALF_UP);

        boolean hoursRisk = avgSelfEmployedHours.compareTo(HOURS_THRESHOLD) > 0;
        boolean incomeRisk = totalSelfEmployedIncome.compareTo(totalEmploymentIncome) > 0;

        RiskLevel riskLevel;
        String riskMessage;

        if (hoursRisk && incomeRisk) {
            riskLevel = RiskLevel.CRITICAL;
            riskMessage = "Umklassifizierungsrisiko: Selbständige Tätigkeit überwiegt bei Arbeitszeit ("
                    + avgSelfEmployedHours + " Std./Woche) und Einkommen. §5 Abs. 5 SGB V prüfen!";
        } else if (hoursRisk) {
            riskLevel = RiskLevel.WARNING;
            riskMessage = "Warnung: Selbständige Arbeitszeit (" + avgSelfEmployedHours
                    + " Std./Woche) überschreitet 20-Stunden-Grenze.";
        } else if (incomeRisk) {
            riskLevel = RiskLevel.WARNING;
            riskMessage = "Warnung: Selbständiges Einkommen übersteigt Arbeitnehmereinkommen.";
        } else {
            riskLevel = RiskLevel.SAFE;
            riskMessage = "Anstellung ist Haupttätigkeit. Selbständigkeit gilt als Nebentätigkeit.";
        }

        return new SocialInsuranceStatus(year,
                avgEmploymentHours, avgSelfEmployedHours,
                totalEmploymentIncome, totalSelfEmployedIncome,
                hoursRisk, incomeRisk, riskLevel, riskMessage,
                monthlyData);
    }

    /**
     * Create or update a monthly social insurance entry.
     */
    @Transactional
    public SocialInsuranceEntry upsertEntry(AppUser user, int year, int month,
                                            BigDecimal employmentHoursWeekly,
                                            BigDecimal selfEmployedHoursWeekly,
                                            BigDecimal employmentIncome,
                                            BigDecimal selfEmployedIncome) {
        return entryRepository.findByUserIdAndYearAndMonth(
                        user.getId(), (short) year, (short) month)
                .map(entry -> {
                    entry.update(employmentHoursWeekly, selfEmployedHoursWeekly,
                            employmentIncome, selfEmployedIncome);
                    return entry;
                })
                .orElseGet(() -> entryRepository.save(new SocialInsuranceEntry(
                        user, year, month,
                        employmentHoursWeekly, selfEmployedHoursWeekly,
                        employmentIncome, selfEmployedIncome)));
    }
}
