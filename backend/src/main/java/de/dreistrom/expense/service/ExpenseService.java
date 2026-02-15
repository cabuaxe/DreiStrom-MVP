package de.dreistrom.expense.service;

import de.dreistrom.audit.service.AuditLogService;
import de.dreistrom.common.controller.EntityNotFoundException;
import de.dreistrom.common.domain.AppUser;
import de.dreistrom.expense.domain.AllocationRule;
import de.dreistrom.expense.domain.DepreciationAsset;
import de.dreistrom.expense.domain.ExpenseEntry;
import de.dreistrom.expense.event.DepreciationAssetCreated;
import de.dreistrom.expense.event.ExpenseEntryCreated;
import de.dreistrom.expense.event.ExpenseEntryModified;
import de.dreistrom.expense.repository.AllocationRuleRepository;
import de.dreistrom.expense.repository.DepreciationAssetRepository;
import de.dreistrom.expense.repository.ExpenseEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    /** GWG threshold per §6 Abs. 2 EStG: net cost up to 800 EUR */
    private static final BigDecimal GWG_THRESHOLD = new BigDecimal("800.00");

    /** Default useful life for computer equipment (BMF AfA-Tabelle) */
    private static final int DEFAULT_USEFUL_LIFE_MONTHS = 36;

    private final ExpenseEntryRepository expenseEntryRepository;
    private final AllocationRuleRepository allocationRuleRepository;
    private final DepreciationAssetRepository depreciationAssetRepository;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public ExpenseEntry create(AppUser user, BigDecimal amount, String category,
                               LocalDate entryDate, Long allocationRuleId,
                               Long receiptDocId, String description) {
        AllocationRule rule = resolveAllocationRule(allocationRuleId, user.getId());

        ExpenseEntry entry = new ExpenseEntry(user, amount, category, entryDate,
                rule, receiptDocId, description);
        ExpenseEntry saved = expenseEntryRepository.save(entry);

        boolean gwg = isGwg(amount);

        ExpenseEntryCreated event = new ExpenseEntryCreated(saved, gwg);
        auditLogService.persist(event);
        eventPublisher.publishEvent(event);

        // Auto-create depreciation asset for items > 800 EUR (not GWG)
        if (!gwg && amount.compareTo(GWG_THRESHOLD) > 0) {
            createDepreciationAsset(user, category, entryDate, amount,
                    DEFAULT_USEFUL_LIFE_MONTHS, saved);
        }

        return saved;
    }

    @Transactional
    public ExpenseEntry update(Long entryId, Long userId, BigDecimal amount,
                               String category, LocalDate entryDate,
                               Long allocationRuleId, Long receiptDocId,
                               String description) {
        ExpenseEntry entry = getOwnedEntry(entryId, userId);

        BigDecimal beforeAmount = entry.getAmount();
        String beforeCategory = entry.getCategory();
        LocalDate beforeDate = entry.getEntryDate();

        AllocationRule rule = resolveAllocationRule(allocationRuleId, userId);
        entry.update(amount, category, entryDate, rule, receiptDocId, description);

        ExpenseEntryModified modifiedEvent = new ExpenseEntryModified(
                entryId, beforeAmount, amount, beforeCategory, category,
                beforeDate, entryDate);
        auditLogService.persist(modifiedEvent);
        eventPublisher.publishEvent(modifiedEvent);

        return entry;
    }

    @Transactional(readOnly = true)
    public ExpenseEntry getById(Long entryId, Long userId) {
        return getOwnedEntry(entryId, userId);
    }

    @Transactional(readOnly = true)
    public List<ExpenseEntry> listAll(Long userId) {
        return expenseEntryRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<ExpenseEntry> listByCategory(Long userId, String category) {
        return expenseEntryRepository.findByUserIdAndCategory(userId, category);
    }

    @Transactional(readOnly = true)
    public List<ExpenseEntry> listByDateRange(Long userId, LocalDate from, LocalDate to) {
        return expenseEntryRepository.findByUserIdAndEntryDateBetween(userId, from, to);
    }

    @Transactional(readOnly = true)
    public List<ExpenseEntry> listByCategoryAndDateRange(Long userId, String category,
                                                         LocalDate from, LocalDate to) {
        return expenseEntryRepository.findByUserIdAndCategoryAndEntryDateBetween(
                userId, category, from, to);
    }

    @Transactional
    public void delete(Long entryId, Long userId) {
        ExpenseEntry entry = getOwnedEntry(entryId, userId);
        expenseEntryRepository.delete(entry);
    }

    /**
     * Check if a net amount qualifies as GWG (Geringwertiges Wirtschaftsgut)
     * per §6 Abs. 2 EStG: net cost up to 800 EUR.
     */
    public boolean isGwg(BigDecimal netAmount) {
        return netAmount.compareTo(GWG_THRESHOLD) <= 0;
    }

    @Transactional
    public DepreciationAsset createDepreciationAsset(AppUser user, String name,
                                                     LocalDate acquisitionDate,
                                                     BigDecimal netCost,
                                                     int usefulLifeMonths,
                                                     ExpenseEntry expenseEntry) {
        BigDecimal annualAfa = netCost.divide(
                new BigDecimal(usefulLifeMonths).divide(
                        new BigDecimal("12"), 10, RoundingMode.HALF_UP),
                2, RoundingMode.HALF_UP);

        DepreciationAsset asset = new DepreciationAsset(user, name, acquisitionDate,
                netCost, usefulLifeMonths, annualAfa, expenseEntry);
        DepreciationAsset saved = depreciationAssetRepository.save(asset);

        DepreciationAssetCreated event = new DepreciationAssetCreated(saved);
        auditLogService.persist(event);
        eventPublisher.publishEvent(event);

        return saved;
    }

    private AllocationRule resolveAllocationRule(Long allocationRuleId, Long userId) {
        if (allocationRuleId == null) {
            return null;
        }
        AllocationRule rule = allocationRuleRepository.findById(allocationRuleId)
                .orElseThrow(() -> new EntityNotFoundException("AllocationRule", allocationRuleId));
        if (!rule.getUser().getId().equals(userId)) {
            throw new EntityNotFoundException("AllocationRule", allocationRuleId);
        }
        return rule;
    }

    private ExpenseEntry getOwnedEntry(Long entryId, Long userId) {
        ExpenseEntry entry = expenseEntryRepository.findById(entryId)
                .orElseThrow(() -> new EntityNotFoundException("ExpenseEntry", entryId));
        if (!entry.getUser().getId().equals(userId)) {
            throw new EntityNotFoundException("ExpenseEntry", entryId);
        }
        return entry;
    }
}
