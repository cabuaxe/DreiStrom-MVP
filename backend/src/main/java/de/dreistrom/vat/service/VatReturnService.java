package de.dreistrom.vat.service;

import de.dreistrom.common.controller.EntityNotFoundException;
import de.dreistrom.common.domain.AppUser;
import de.dreistrom.vat.domain.PeriodType;
import de.dreistrom.vat.domain.VatReturn;
import de.dreistrom.vat.domain.VatReturnStatus;
import de.dreistrom.vat.dto.VatSummary;
import de.dreistrom.vat.repository.VatReturnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates and manages Umsatzsteuer-Voranmeldung (USt-VA) data.
 * Supports monthly (first year) or quarterly cadence.
 */
@Service
@RequiredArgsConstructor
public class VatReturnService {

    private final VatReturnRepository vatReturnRepository;
    private final VatCalculationService vatCalculationService;

    /**
     * Generate or update a VAT return for a specific period.
     * Aggregates output VAT, input VAT, and net payable from invoices and expenses.
     */
    @Transactional
    public VatReturn generateForPeriod(AppUser user, int year, PeriodType periodType,
                                       int periodNumber, boolean kleinunternehmer) {
        LocalDate from = periodStart(year, periodType, periodNumber);
        LocalDate to = periodEnd(year, periodType, periodNumber);

        VatSummary summary = vatCalculationService.calculate(
                user.getId(), from, to, kleinunternehmer);

        VatReturn existing = vatReturnRepository
                .findByUserIdAndYearAndPeriodTypeAndPeriodNumber(
                        user.getId(), (short) year, periodType, (short) periodNumber)
                .orElse(null);

        if (existing != null) {
            if (existing.getStatus() != VatReturnStatus.DRAFT) {
                throw new IllegalStateException(
                        "Cannot regenerate a VAT return with status " + existing.getStatus());
            }
            existing.updateAmounts(summary.outputVat(), summary.inputVat(), summary.netPayable());
            return existing;
        }

        return vatReturnRepository.save(new VatReturn(
                user, year, periodType, periodNumber,
                summary.outputVat(), summary.inputVat(), summary.netPayable()));
    }

    /**
     * Generate VAT returns for all periods in a year.
     */
    @Transactional
    public List<VatReturn> generateForYear(AppUser user, int year, PeriodType periodType,
                                           boolean kleinunternehmer) {
        int periods = periodsInYear(periodType);
        List<VatReturn> returns = new ArrayList<>(periods);
        for (int p = 1; p <= periods; p++) {
            returns.add(generateForPeriod(user, year, periodType, p, kleinunternehmer));
        }
        return returns;
    }

    @Transactional(readOnly = true)
    public List<VatReturn> listByYear(Long userId, int year) {
        return vatReturnRepository.findByUserIdAndYear(userId, (short) year);
    }

    @Transactional(readOnly = true)
    public List<VatReturn> listByYearAndType(Long userId, int year, PeriodType periodType) {
        return vatReturnRepository.findByUserIdAndYearAndPeriodType(
                userId, (short) year, periodType);
    }

    @Transactional(readOnly = true)
    public VatReturn getById(Long id, Long userId) {
        VatReturn vr = vatReturnRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("VatReturn", id));
        if (!vr.getUser().getId().equals(userId)) {
            throw new EntityNotFoundException("VatReturn", id);
        }
        return vr;
    }

    @Transactional
    public VatReturn submit(Long id, Long userId, LocalDate submissionDate) {
        VatReturn vr = getById(id, userId);
        if (vr.getStatus() != VatReturnStatus.DRAFT) {
            throw new IllegalStateException(
                    "Can only submit DRAFT returns, current status: " + vr.getStatus());
        }
        vr.submit(submissionDate);
        return vr;
    }

    @Transactional
    public VatReturn updateStatus(Long id, Long userId, VatReturnStatus status) {
        VatReturn vr = getById(id, userId);
        vr.updateStatus(status);
        return vr;
    }

    // ── Period calculation helpers ─────────────────────────────────────

    static LocalDate periodStart(int year, PeriodType type, int number) {
        return switch (type) {
            case MONTHLY -> LocalDate.of(year, number, 1);
            case QUARTERLY -> LocalDate.of(year, (number - 1) * 3 + 1, 1);
            case ANNUAL -> LocalDate.of(year, 1, 1);
        };
    }

    static LocalDate periodEnd(int year, PeriodType type, int number) {
        return switch (type) {
            case MONTHLY -> LocalDate.of(year, number, 1).plusMonths(1).minusDays(1);
            case QUARTERLY -> LocalDate.of(year, (number - 1) * 3 + 1, 1)
                    .plusMonths(3).minusDays(1);
            case ANNUAL -> LocalDate.of(year, 12, 31);
        };
    }

    private static int periodsInYear(PeriodType type) {
        return switch (type) {
            case MONTHLY -> 12;
            case QUARTERLY -> 4;
            case ANNUAL -> 1;
        };
    }
}
