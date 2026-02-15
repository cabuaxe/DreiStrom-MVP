package de.dreistrom.expense.service;

import de.dreistrom.audit.service.AuditLogService;
import de.dreistrom.common.controller.EntityNotFoundException;
import de.dreistrom.expense.domain.AllocationRule;
import de.dreistrom.expense.domain.DepreciationAsset;
import de.dreistrom.expense.domain.ExpenseEntry;
import de.dreistrom.expense.repository.DepreciationAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DepreciationService {

    private final DepreciationAssetRepository depreciationAssetRepository;
    private final AuditLogService auditLogService;

    /**
     * Compute depreciation for a specific calendar year using linear method
     * with pro-rata for acquisition and end of useful life.
     *
     * German tax rule: depreciation starts in the month of acquisition,
     * each month counts fully (Monatsprinzip).
     */
    public BigDecimal computeDepreciationForYear(DepreciationAsset asset, int year) {
        LocalDate acquisitionDate = asset.getAcquisitionDate();
        int acquisitionYear = acquisitionDate.getYear();
        int acquisitionMonth = acquisitionDate.getMonthValue();
        int usefulLifeMonths = asset.getUsefulLifeMonths();

        // End of depreciation: acquisitionMonth + usefulLifeMonths - 1
        LocalDate endDate = acquisitionDate.withDayOfMonth(1)
                .plusMonths(usefulLifeMonths - 1);
        int endYear = endDate.getYear();
        int endMonth = endDate.getMonthValue();

        // If disposed, depreciation stops in the disposal month
        if (asset.isDisposed()) {
            LocalDate disposalDate = asset.getDisposalDate();
            if (disposalDate.getYear() < year) {
                return BigDecimal.ZERO;
            }
            if (disposalDate.getYear() == year) {
                endYear = year;
                endMonth = disposalDate.getMonthValue();
            }
        }

        if (year < acquisitionYear || year > endYear) {
            return BigDecimal.ZERO;
        }

        BigDecimal monthlyAfa = asset.getNetCost().divide(
                new BigDecimal(usefulLifeMonths), 10, RoundingMode.HALF_UP);

        int monthsInYear;
        if (year == acquisitionYear && year == endYear) {
            // Acquisition and end in same year
            monthsInYear = endMonth - acquisitionMonth + 1;
        } else if (year == acquisitionYear) {
            // First year: from acquisition month to December
            monthsInYear = 12 - acquisitionMonth + 1;
        } else if (year == endYear) {
            // Last year: from January to end month
            monthsInYear = endMonth;
        } else {
            // Full year
            monthsInYear = 12;
        }

        return monthlyAfa.multiply(new BigDecimal(monthsInYear))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Compute remaining book value as of a given date.
     */
    public BigDecimal computeRemainingBookValue(DepreciationAsset asset, LocalDate asOfDate) {
        if (asset.isDisposed() && !asOfDate.isBefore(asset.getDisposalDate())) {
            return BigDecimal.ZERO;
        }

        int acquisitionYear = asset.getAcquisitionDate().getYear();
        int asOfYear = asOfDate.getYear();

        BigDecimal totalDepreciated = BigDecimal.ZERO;
        for (int year = acquisitionYear; year <= asOfYear; year++) {
            totalDepreciated = totalDepreciated.add(computeDepreciationForYear(asset, year));
        }

        BigDecimal remaining = asset.getNetCost().subtract(totalDepreciated);
        return remaining.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Generate full depreciation schedule (year-by-year breakdown).
     */
    public List<DepreciationYearEntry> computeSchedule(DepreciationAsset asset) {
        int acquisitionYear = asset.getAcquisitionDate().getYear();
        int acquisitionMonth = asset.getAcquisitionDate().getMonthValue();

        LocalDate endDate = asset.getAcquisitionDate().withDayOfMonth(1)
                .plusMonths(asset.getUsefulLifeMonths() - 1);
        int endYear = endDate.getYear();

        if (asset.isDisposed() && asset.getDisposalDate().getYear() < endYear) {
            endYear = asset.getDisposalDate().getYear();
        }

        List<DepreciationYearEntry> schedule = new ArrayList<>();
        BigDecimal remaining = asset.getNetCost();

        for (int year = acquisitionYear; year <= endYear; year++) {
            BigDecimal yearDepreciation = computeDepreciationForYear(asset, year);
            remaining = remaining.subtract(yearDepreciation)
                    .max(BigDecimal.ZERO)
                    .setScale(2, RoundingMode.HALF_UP);
            schedule.add(new DepreciationYearEntry(year, yearDepreciation, remaining));
        }

        return schedule;
    }

    /**
     * Dispose an asset: sets disposal date, remaining book value becomes zero.
     */
    @Transactional
    public DepreciationAsset dispose(Long assetId, Long userId, LocalDate disposalDate) {
        DepreciationAsset asset = getOwnedAsset(assetId, userId);

        if (asset.isDisposed()) {
            throw new IllegalStateException(
                    "Asset already disposed on: " + asset.getDisposalDate());
        }
        if (disposalDate.isBefore(asset.getAcquisitionDate())) {
            throw new IllegalArgumentException(
                    "Disposal date cannot be before acquisition date");
        }

        asset.dispose(disposalDate);
        return asset;
    }

    /**
     * Compute total depreciation per stream for a given year.
     * Uses allocation_rule from the linked expense_entry to split depreciation.
     * Assets without an allocation rule are reported as unallocated in the total only.
     */
    @Transactional(readOnly = true)
    public StreamDepreciationSummary computeStreamTotalsForYear(Long userId, int year) {
        List<DepreciationAsset> assets = depreciationAssetRepository.findByUserId(userId);

        BigDecimal freiberuf = BigDecimal.ZERO;
        BigDecimal gewerbe = BigDecimal.ZERO;
        BigDecimal personal = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;

        for (DepreciationAsset asset : assets) {
            BigDecimal yearDepreciation = computeDepreciationForYear(asset, year);
            if (yearDepreciation.signum() == 0) {
                continue;
            }

            total = total.add(yearDepreciation);

            ExpenseEntry expense = asset.getExpenseEntry();
            AllocationRule rule = expense != null ? expense.getAllocationRule() : null;
            if (rule != null) {
                freiberuf = freiberuf.add(yearDepreciation
                        .multiply(new BigDecimal(rule.getFreiberufPct()))
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
                gewerbe = gewerbe.add(yearDepreciation
                        .multiply(new BigDecimal(rule.getGewerbePct()))
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
                personal = personal.add(yearDepreciation
                        .multiply(new BigDecimal(rule.getPersonalPct()))
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP));
            }
        }

        return new StreamDepreciationSummary(
                freiberuf.setScale(2, RoundingMode.HALF_UP),
                gewerbe.setScale(2, RoundingMode.HALF_UP),
                personal.setScale(2, RoundingMode.HALF_UP),
                total.setScale(2, RoundingMode.HALF_UP));
    }

    @Transactional(readOnly = true)
    public DepreciationAsset getById(Long assetId, Long userId) {
        return getOwnedAsset(assetId, userId);
    }

    @Transactional(readOnly = true)
    public List<DepreciationAsset> listAll(Long userId) {
        return depreciationAssetRepository.findByUserId(userId);
    }

    private DepreciationAsset getOwnedAsset(Long assetId, Long userId) {
        DepreciationAsset asset = depreciationAssetRepository.findById(assetId)
                .orElseThrow(() -> new EntityNotFoundException("DepreciationAsset", assetId));
        if (!asset.getUser().getId().equals(userId)) {
            throw new EntityNotFoundException("DepreciationAsset", assetId);
        }
        return asset;
    }
}
