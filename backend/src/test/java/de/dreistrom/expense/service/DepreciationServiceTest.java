package de.dreistrom.expense.service;

import de.dreistrom.common.controller.EntityNotFoundException;
import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.repository.AppUserRepository;
import de.dreistrom.expense.domain.AllocationRule;
import de.dreistrom.expense.domain.DepreciationAsset;
import de.dreistrom.expense.domain.ExpenseEntry;
import de.dreistrom.expense.repository.AllocationRuleRepository;
import de.dreistrom.expense.repository.DepreciationAssetRepository;
import de.dreistrom.expense.repository.ExpenseEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class DepreciationServiceTest {

    @Autowired
    private DepreciationService depreciationService;

    @Autowired
    private DepreciationAssetRepository depreciationAssetRepository;

    @Autowired
    private ExpenseEntryRepository expenseEntryRepository;

    @Autowired
    private AllocationRuleRepository allocationRuleRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private AppUser user;

    @BeforeEach
    void setUp() {
        depreciationAssetRepository.deleteAll();
        expenseEntryRepository.deleteAll();
        allocationRuleRepository.deleteAll();
        appUserRepository.deleteAll();
        user = appUserRepository.save(new AppUser(
                "afa@dreistrom.de",
                passwordEncoder.encode("test1234"),
                "AfA Tester"));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("afa@dreistrom.de", null, List.of()));
    }

    // -- Full-year depreciation --

    @Test
    void computeDepreciationForYear_fullYear_linear() {
        // Asset: 3600 EUR, 36 months (3 years), acquired Jan 2026
        // Annual AfA = 3600 / 3 = 1200
        DepreciationAsset asset = depreciationAssetRepository.save(new DepreciationAsset(
                user, "Laptop", LocalDate.of(2026, 1, 1),
                new BigDecimal("3600.00"), 36, new BigDecimal("1200.00")));

        assertThat(depreciationService.computeDepreciationForYear(asset, 2026))
                .isEqualByComparingTo("1200.00");
        assertThat(depreciationService.computeDepreciationForYear(asset, 2027))
                .isEqualByComparingTo("1200.00");
        assertThat(depreciationService.computeDepreciationForYear(asset, 2028))
                .isEqualByComparingTo("1200.00");
    }

    @Test
    void computeDepreciationForYear_beforeAndAfterUsefulLife_returnsZero() {
        DepreciationAsset asset = depreciationAssetRepository.save(new DepreciationAsset(
                user, "Laptop", LocalDate.of(2026, 1, 1),
                new BigDecimal("3600.00"), 36, new BigDecimal("1200.00")));

        assertThat(depreciationService.computeDepreciationForYear(asset, 2025))
                .isEqualByComparingTo("0");
        assertThat(depreciationService.computeDepreciationForYear(asset, 2029))
                .isEqualByComparingTo("0");
    }

    // -- Pro-rata (partial year acquisition) --

    @Test
    void computeDepreciationForYear_proRata_acquisitionMidYear() {
        // Acquired July 2026, 36 months
        // Monthly AfA = 3600 / 36 = 100
        // 2026: 6 months (Jul-Dec) = 600
        // 2027: 12 months = 1200
        // 2028: 12 months = 1200
        // 2029: 6 months (Jan-Jun) = 600
        DepreciationAsset asset = depreciationAssetRepository.save(new DepreciationAsset(
                user, "Monitor", LocalDate.of(2026, 7, 15),
                new BigDecimal("3600.00"), 36, new BigDecimal("1200.00")));

        assertThat(depreciationService.computeDepreciationForYear(asset, 2026))
                .isEqualByComparingTo("600.00");
        assertThat(depreciationService.computeDepreciationForYear(asset, 2027))
                .isEqualByComparingTo("1200.00");
        assertThat(depreciationService.computeDepreciationForYear(asset, 2028))
                .isEqualByComparingTo("1200.00");
        assertThat(depreciationService.computeDepreciationForYear(asset, 2029))
                .isEqualByComparingTo("600.00");
    }

    @Test
    void computeDepreciationForYear_proRata_acquisitionInDecember() {
        // Acquired December 2026, 36 months
        // Monthly AfA = 2400 / 36 = 66.666...
        // 2026: 1 month = 66.67
        // 2027-2028: 12 months each = 800.00
        // 2029: 11 months = 733.33
        DepreciationAsset asset = depreciationAssetRepository.save(new DepreciationAsset(
                user, "Schreibtisch", LocalDate.of(2026, 12, 1),
                new BigDecimal("2400.00"), 36, new BigDecimal("800.00")));

        assertThat(depreciationService.computeDepreciationForYear(asset, 2026))
                .isEqualByComparingTo("66.67");
        assertThat(depreciationService.computeDepreciationForYear(asset, 2027))
                .isEqualByComparingTo("800.00");
        assertThat(depreciationService.computeDepreciationForYear(asset, 2028))
                .isEqualByComparingTo("800.00");
        assertThat(depreciationService.computeDepreciationForYear(asset, 2029))
                .isEqualByComparingTo("733.33");
    }

    // -- Remaining book value --

    @Test
    void computeRemainingBookValue_afterFirstYear() {
        DepreciationAsset asset = depreciationAssetRepository.save(new DepreciationAsset(
                user, "Laptop", LocalDate.of(2026, 1, 1),
                new BigDecimal("3600.00"), 36, new BigDecimal("1200.00")));

        assertThat(depreciationService.computeRemainingBookValue(asset,
                LocalDate.of(2026, 12, 31)))
                .isEqualByComparingTo("2400.00");
        assertThat(depreciationService.computeRemainingBookValue(asset,
                LocalDate.of(2028, 12, 31)))
                .isEqualByComparingTo("0.00");
    }

    @Test
    void computeRemainingBookValue_proRata_midYear() {
        // Acquired July 2026, 3600 EUR, 36 months
        // After 2026: 3600 - 600 = 3000
        // After 2027: 3000 - 1200 = 1800
        DepreciationAsset asset = depreciationAssetRepository.save(new DepreciationAsset(
                user, "Monitor", LocalDate.of(2026, 7, 15),
                new BigDecimal("3600.00"), 36, new BigDecimal("1200.00")));

        assertThat(depreciationService.computeRemainingBookValue(asset,
                LocalDate.of(2026, 12, 31)))
                .isEqualByComparingTo("3000.00");
        assertThat(depreciationService.computeRemainingBookValue(asset,
                LocalDate.of(2027, 12, 31)))
                .isEqualByComparingTo("1800.00");
        assertThat(depreciationService.computeRemainingBookValue(asset,
                LocalDate.of(2029, 12, 31)))
                .isEqualByComparingTo("0.00");
    }

    // -- Depreciation schedule --

    @Test
    void computeSchedule_fullYears() {
        DepreciationAsset asset = depreciationAssetRepository.save(new DepreciationAsset(
                user, "Laptop", LocalDate.of(2026, 1, 1),
                new BigDecimal("3600.00"), 36, new BigDecimal("1200.00")));

        List<DepreciationYearEntry> schedule = depreciationService.computeSchedule(asset);

        assertThat(schedule).hasSize(3);
        assertThat(schedule.get(0).year()).isEqualTo(2026);
        assertThat(schedule.get(0).depreciation()).isEqualByComparingTo("1200.00");
        assertThat(schedule.get(0).remainingBookValue()).isEqualByComparingTo("2400.00");
        assertThat(schedule.get(2).remainingBookValue()).isEqualByComparingTo("0.00");
    }

    @Test
    void computeSchedule_proRata() {
        DepreciationAsset asset = depreciationAssetRepository.save(new DepreciationAsset(
                user, "Monitor", LocalDate.of(2026, 7, 15),
                new BigDecimal("3600.00"), 36, new BigDecimal("1200.00")));

        List<DepreciationYearEntry> schedule = depreciationService.computeSchedule(asset);

        assertThat(schedule).hasSize(4);
        assertThat(schedule.get(0).year()).isEqualTo(2026);
        assertThat(schedule.get(0).depreciation()).isEqualByComparingTo("600.00");
        assertThat(schedule.get(3).year()).isEqualTo(2029);
        assertThat(schedule.get(3).depreciation()).isEqualByComparingTo("600.00");
        assertThat(schedule.get(3).remainingBookValue()).isEqualByComparingTo("0.00");
    }

    // -- Disposal handling --

    @Test
    void dispose_zeroesRemainingBookValue() {
        DepreciationAsset asset = depreciationAssetRepository.save(new DepreciationAsset(
                user, "Laptop", LocalDate.of(2026, 1, 1),
                new BigDecimal("3600.00"), 36, new BigDecimal("1200.00")));

        depreciationService.dispose(asset.getId(), user.getId(), LocalDate.of(2027, 6, 15));

        assertThat(asset.isDisposed()).isTrue();
        assertThat(asset.getDisposalDate()).isEqualTo(LocalDate.of(2027, 6, 15));

        // After disposal date, remaining book value is zero
        assertThat(depreciationService.computeRemainingBookValue(asset,
                LocalDate.of(2027, 6, 15)))
                .isEqualByComparingTo("0.00");
    }

    @Test
    void dispose_stopsDepreciation_inDisposalYear() {
        DepreciationAsset asset = depreciationAssetRepository.save(new DepreciationAsset(
                user, "Laptop", LocalDate.of(2026, 1, 1),
                new BigDecimal("3600.00"), 36, new BigDecimal("1200.00")));

        depreciationService.dispose(asset.getId(), user.getId(), LocalDate.of(2027, 6, 15));

        // 2026: full year = 1200
        assertThat(depreciationService.computeDepreciationForYear(asset, 2026))
                .isEqualByComparingTo("1200.00");
        // 2027: 6 months (Jan-Jun) = 600
        assertThat(depreciationService.computeDepreciationForYear(asset, 2027))
                .isEqualByComparingTo("600.00");
        // 2028: nothing, already disposed
        assertThat(depreciationService.computeDepreciationForYear(asset, 2028))
                .isEqualByComparingTo("0");
    }

    @Test
    void dispose_scheduleStopsAtDisposalYear() {
        DepreciationAsset asset = depreciationAssetRepository.save(new DepreciationAsset(
                user, "Laptop", LocalDate.of(2026, 1, 1),
                new BigDecimal("3600.00"), 36, new BigDecimal("1200.00")));

        depreciationService.dispose(asset.getId(), user.getId(), LocalDate.of(2027, 6, 15));

        List<DepreciationYearEntry> schedule = depreciationService.computeSchedule(asset);
        assertThat(schedule).hasSize(2);
        assertThat(schedule.get(1).year()).isEqualTo(2027);
        assertThat(schedule.get(1).depreciation()).isEqualByComparingTo("600.00");
    }

    @Test
    void dispose_alreadyDisposed_throwsException() {
        DepreciationAsset asset = depreciationAssetRepository.save(new DepreciationAsset(
                user, "Laptop", LocalDate.of(2026, 1, 1),
                new BigDecimal("3600.00"), 36, new BigDecimal("1200.00")));

        depreciationService.dispose(asset.getId(), user.getId(), LocalDate.of(2027, 6, 15));

        assertThatThrownBy(() -> depreciationService.dispose(
                asset.getId(), user.getId(), LocalDate.of(2027, 9, 1)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already disposed");
    }

    @Test
    void dispose_beforeAcquisition_throwsException() {
        DepreciationAsset asset = depreciationAssetRepository.save(new DepreciationAsset(
                user, "Laptop", LocalDate.of(2026, 6, 1),
                new BigDecimal("3600.00"), 36, new BigDecimal("1200.00")));

        assertThatThrownBy(() -> depreciationService.dispose(
                asset.getId(), user.getId(), LocalDate.of(2025, 12, 31)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("before acquisition");
    }

    @Test
    void dispose_otherUsersAsset_throwsEntityNotFound() {
        AppUser otherUser = appUserRepository.save(new AppUser(
                "other@dreistrom.de",
                passwordEncoder.encode("test1234"),
                "Other User"));
        DepreciationAsset asset = depreciationAssetRepository.save(new DepreciationAsset(
                otherUser, "Laptop", LocalDate.of(2026, 1, 1),
                new BigDecimal("3600.00"), 36, new BigDecimal("1200.00")));

        assertThatThrownBy(() -> depreciationService.dispose(
                asset.getId(), user.getId(), LocalDate.of(2027, 6, 15)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // -- Per-stream totals --

    @Test
    void computeStreamTotalsForYear_splitsCorrectly() {
        AllocationRule rule = allocationRuleRepository.save(new AllocationRule(user,
                "Office Split", (short) 60, (short) 30, (short) 10));

        ExpenseEntry expense = expenseEntryRepository.save(new ExpenseEntry(user,
                new BigDecimal("3600.00"), "Arbeitsmittel", LocalDate.of(2026, 1, 1),
                rule, null, null));

        depreciationAssetRepository.save(new DepreciationAsset(user, "Laptop",
                LocalDate.of(2026, 1, 1), new BigDecimal("3600.00"),
                36, new BigDecimal("1200.00"), expense));

        StreamDepreciationSummary summary = depreciationService
                .computeStreamTotalsForYear(user.getId(), 2026);

        // Total depreciation = 1200.00 for full year
        assertThat(summary.total()).isEqualByComparingTo("1200.00");
        // Freiberuf: 1200 * 60% = 720
        assertThat(summary.freiberuf()).isEqualByComparingTo("720.00");
        // Gewerbe: 1200 * 30% = 360
        assertThat(summary.gewerbe()).isEqualByComparingTo("360.00");
        // Personal: 1200 * 10% = 120
        assertThat(summary.personal()).isEqualByComparingTo("120.00");
    }

    @Test
    void computeStreamTotalsForYear_multipleAssets() {
        AllocationRule rule = allocationRuleRepository.save(new AllocationRule(user,
                "Even Split", (short) 50, (short) 50, (short) 0));

        ExpenseEntry expense1 = expenseEntryRepository.save(new ExpenseEntry(user,
                new BigDecimal("2400.00"), "Arbeitsmittel", LocalDate.of(2026, 1, 1),
                rule, null, null));
        ExpenseEntry expense2 = expenseEntryRepository.save(new ExpenseEntry(user,
                new BigDecimal("1200.00"), "Büroausstattung", LocalDate.of(2026, 1, 1),
                rule, null, null));

        depreciationAssetRepository.save(new DepreciationAsset(user, "Laptop",
                LocalDate.of(2026, 1, 1), new BigDecimal("2400.00"),
                36, new BigDecimal("800.00"), expense1));
        depreciationAssetRepository.save(new DepreciationAsset(user, "Monitor",
                LocalDate.of(2026, 1, 1), new BigDecimal("1200.00"),
                36, new BigDecimal("400.00"), expense2));

        StreamDepreciationSummary summary = depreciationService
                .computeStreamTotalsForYear(user.getId(), 2026);

        // Total: 800 + 400 = 1200
        assertThat(summary.total()).isEqualByComparingTo("1200.00");
        // Freiberuf: 1200 * 50% = 600
        assertThat(summary.freiberuf()).isEqualByComparingTo("600.00");
        // Gewerbe: 1200 * 50% = 600
        assertThat(summary.gewerbe()).isEqualByComparingTo("600.00");
        assertThat(summary.personal()).isEqualByComparingTo("0.00");
    }

    @Test
    void computeStreamTotalsForYear_unallocatedAsset_includedInTotalOnly() {
        // Asset without expense entry / allocation rule
        depreciationAssetRepository.save(new DepreciationAsset(user, "Drucker",
                LocalDate.of(2026, 1, 1), new BigDecimal("1800.00"),
                36, new BigDecimal("600.00")));

        StreamDepreciationSummary summary = depreciationService
                .computeStreamTotalsForYear(user.getId(), 2026);

        assertThat(summary.total()).isEqualByComparingTo("600.00");
        assertThat(summary.freiberuf()).isEqualByComparingTo("0.00");
        assertThat(summary.gewerbe()).isEqualByComparingTo("0.00");
        assertThat(summary.personal()).isEqualByComparingTo("0.00");
    }

    @Test
    void computeStreamTotalsForYear_noAssets_returnsZeroes() {
        StreamDepreciationSummary summary = depreciationService
                .computeStreamTotalsForYear(user.getId(), 2026);

        assertThat(summary.total()).isEqualByComparingTo("0.00");
        assertThat(summary.freiberuf()).isEqualByComparingTo("0.00");
    }
}
