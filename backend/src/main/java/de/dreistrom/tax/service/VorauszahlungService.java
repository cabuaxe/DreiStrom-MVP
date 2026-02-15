package de.dreistrom.tax.service;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.income.repository.IncomeEntryRepository;
import de.dreistrom.tax.domain.Vorauszahlung;
import de.dreistrom.tax.domain.VorauszahlungStatus;
import de.dreistrom.tax.dto.VorauszahlungSchedule;
import de.dreistrom.tax.dto.VorauszahlungSchedule.AdjustmentSuggestion;
import de.dreistrom.tax.dto.VorauszahlungSchedule.QuarterPayment;
import de.dreistrom.tax.repository.VorauszahlungRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages quarterly advance tax payment (Vorauszahlung) schedules.
 * <p>
 * German advance tax payments are due on 10 Mar, 10 Jun, 10 Sep, and 10 Dec.
 * The Finanzamt sets the amount based on the previous assessment (Bescheid).
 * If actual income deviates >25% from the assessment basis, an adjustment
 * request (Anpassungsantrag) should be filed.
 */
@Service
@RequiredArgsConstructor
public class VorauszahlungService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal FOUR = new BigDecimal("4");
    private static final BigDecimal DEVIATION_THRESHOLD = new BigDecimal("25");

    /** Quarterly due dates: month/day for Q1-Q4. */
    private static final int[][] DUE_DATES = {{3, 10}, {6, 10}, {9, 10}, {12, 10}};

    private final VorauszahlungRepository vorauszahlungRepository;
    private final IncomeEntryRepository incomeEntryRepository;
    private final Clock clock;

    /**
     * Generate or retrieve the payment schedule for a user and year.
     * Creates Vorauszahlung records if they don't exist.
     *
     * @param user            the user
     * @param year            the tax year
     * @param assessmentBasis annual tax from Finanzamt assessment (Bescheid)
     * @return the full schedule with payment status and adjustment suggestion
     */
    @Transactional
    public VorauszahlungSchedule generateSchedule(AppUser user, int year,
                                                   BigDecimal assessmentBasis) {
        BigDecimal quarterlyAmount = assessmentBasis.divide(FOUR, 2, RoundingMode.HALF_UP);

        List<Vorauszahlung> existing = vorauszahlungRepository
                .findByUserIdAndYearOrderByQuarter(user.getId(), (short) year);

        List<Vorauszahlung> payments;
        if (existing.size() == 4) {
            payments = existing;
            // Update amounts if assessment basis changed
            for (Vorauszahlung v : payments) {
                if (v.getStatus() == VorauszahlungStatus.PENDING) {
                    v.updateAssessmentBasis(assessmentBasis);
                    v.updateAmount(quarterlyAmount);
                }
            }
        } else {
            payments = createSchedule(user, year, assessmentBasis, quarterlyAmount);
        }

        // Check for overdue payments
        markOverduePayments(payments);

        BigDecimal annualTotal = quarterlyAmount.multiply(FOUR);

        List<QuarterPayment> quarterPayments = payments.stream()
                .map(v -> new QuarterPayment(
                        v.getQuarter(),
                        v.getDueDate().toString(),
                        v.getAmount(),
                        v.getPaid(),
                        v.getStatus().name()))
                .toList();

        AdjustmentSuggestion suggestion = checkDeviation(user.getId(), year, assessmentBasis);

        return new VorauszahlungSchedule(
                year, assessmentBasis, quarterlyAmount, annualTotal,
                quarterPayments, suggestion);
    }

    /**
     * Record a payment for a specific quarter.
     */
    @Transactional
    public Vorauszahlung recordPayment(Long userId, int year, int quarter,
                                       BigDecimal paidAmount, LocalDate paidDate) {
        Vorauszahlung v = vorauszahlungRepository
                .findByUserIdAndYearAndQuarter(userId, (short) year, (short) quarter)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No Vorauszahlung found for Q" + quarter + "/" + year));
        v.markPaid(paidAmount, paidDate);
        return v;
    }

    /**
     * Retrieve the schedule for a user and year (read-only).
     */
    @Transactional(readOnly = true)
    public List<Vorauszahlung> getSchedule(Long userId, int year) {
        return vorauszahlungRepository.findByUserIdAndYearOrderByQuarter(userId, (short) year);
    }

    /**
     * Check if actual income deviates significantly from the assessment basis.
     * Suggests an Anpassungsantrag if deviation exceeds 25%.
     */
    public AdjustmentSuggestion checkDeviation(Long userId, int year,
                                                BigDecimal assessmentBasis) {
        if (assessmentBasis.signum() == 0) {
            return AdjustmentSuggestion.none();
        }

        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        Long totalCents = incomeEntryRepository.sumCentsSelfEmployedByDateRange(
                userId, yearStart, yearEnd);
        if (totalCents == null || totalCents == 0L) {
            return AdjustmentSuggestion.none();
        }

        BigDecimal actualIncome = new BigDecimal(totalCents)
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);

        // Project annual income from year-to-date
        LocalDate today = LocalDate.now(clock);
        if (today.getYear() == year && today.getDayOfYear() > 0) {
            int daysInYear = today.isLeapYear() ? 366 : 365;
            actualIncome = actualIncome
                    .multiply(new BigDecimal(daysInYear))
                    .divide(new BigDecimal(today.getDayOfYear()), 2, RoundingMode.HALF_UP);
        }

        BigDecimal deviation = actualIncome.subtract(assessmentBasis)
                .abs()
                .multiply(HUNDRED)
                .divide(assessmentBasis, 2, RoundingMode.HALF_UP);

        boolean recommended = deviation.compareTo(DEVIATION_THRESHOLD) > 0;

        BigDecimal suggestedQuarterly = BigDecimal.ZERO;
        if (recommended) {
            // Rough estimate: apply same effective tax rate to projected income
            suggestedQuarterly = actualIncome.divide(FOUR, 2, RoundingMode.HALF_UP);
        }

        return new AdjustmentSuggestion(
                recommended, actualIncome, assessmentBasis, deviation, suggestedQuarterly);
    }

    private List<Vorauszahlung> createSchedule(AppUser user, int year,
                                                BigDecimal assessmentBasis,
                                                BigDecimal quarterlyAmount) {
        List<Vorauszahlung> payments = new ArrayList<>(4);
        for (int q = 1; q <= 4; q++) {
            LocalDate dueDate = LocalDate.of(year, DUE_DATES[q - 1][0], DUE_DATES[q - 1][1]);
            Vorauszahlung v = new Vorauszahlung(user, year, q, dueDate,
                    assessmentBasis, quarterlyAmount);
            payments.add(vorauszahlungRepository.save(v));
        }
        return payments;
    }

    private void markOverduePayments(List<Vorauszahlung> payments) {
        LocalDate today = LocalDate.now(clock);
        for (Vorauszahlung v : payments) {
            if (v.getStatus() == VorauszahlungStatus.PENDING
                    && v.getDueDate().isBefore(today)) {
                v.markOverdue();
            }
        }
    }
}
