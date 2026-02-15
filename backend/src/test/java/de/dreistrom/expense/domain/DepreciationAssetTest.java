package de.dreistrom.expense.domain;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.repository.AppUserRepository;
import de.dreistrom.expense.repository.AllocationRuleRepository;
import de.dreistrom.expense.repository.DepreciationAssetRepository;
import de.dreistrom.expense.repository.ExpenseEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class DepreciationAssetTest {

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
                "depreciation@dreistrom.de",
                passwordEncoder.encode("test1234"),
                "AfA Tester"));
    }

    @Test
    void persist_andRetrieve_depreciationAsset() {
        DepreciationAsset asset = new DepreciationAsset(user, "MacBook Pro",
                LocalDate.of(2026, 1, 15), new BigDecimal("2499.00"),
                36, new BigDecimal("833.00"));
        DepreciationAsset saved = depreciationAssetRepository.save(asset);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("MacBook Pro");
        assertThat(saved.getAcquisitionDate()).isEqualTo(LocalDate.of(2026, 1, 15));
        assertThat(saved.getNetCost()).isEqualByComparingTo("2499.00");
        assertThat(saved.getUsefulLifeMonths()).isEqualTo(36);
        assertThat(saved.getAnnualAfa()).isEqualByComparingTo("833.00");
        assertThat(saved.getExpenseEntry()).isNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void persist_withExpenseEntryLink() {
        ExpenseEntry expense = expenseEntryRepository.save(new ExpenseEntry(user,
                new BigDecimal("2499.00"), "Arbeitsmittel", LocalDate.of(2026, 1, 15)));

        DepreciationAsset asset = new DepreciationAsset(user, "MacBook Pro",
                LocalDate.of(2026, 1, 15), new BigDecimal("2499.00"),
                36, new BigDecimal("833.00"), expense);
        DepreciationAsset saved = depreciationAssetRepository.save(asset);

        assertThat(saved.getExpenseEntry().getId()).isEqualTo(expense.getId());
    }

    @Test
    void moneyConverter_storesCents_andRestoresEuros() {
        DepreciationAsset asset = new DepreciationAsset(user, "Schreibtisch",
                LocalDate.of(2026, 2, 1), new BigDecimal("799.99"),
                120, new BigDecimal("80.00"));
        depreciationAssetRepository.save(asset);
        depreciationAssetRepository.flush();

        DepreciationAsset fetched = depreciationAssetRepository.findById(asset.getId()).orElseThrow();
        assertThat(fetched.getNetCost()).isEqualByComparingTo("799.99");
        assertThat(fetched.getAnnualAfa()).isEqualByComparingTo("80.00");
    }

    @Test
    void findByUserId_filtersCorrectly() {
        AppUser otherUser = appUserRepository.save(new AppUser(
                "other@dreistrom.de",
                passwordEncoder.encode("test1234"),
                "Other User"));

        depreciationAssetRepository.save(new DepreciationAsset(user, "Laptop",
                LocalDate.of(2026, 1, 1), new BigDecimal("1500.00"), 36, new BigDecimal("500.00")));
        depreciationAssetRepository.save(new DepreciationAsset(user, "Monitor",
                LocalDate.of(2026, 2, 1), new BigDecimal("600.00"), 36, new BigDecimal("200.00")));
        depreciationAssetRepository.save(new DepreciationAsset(otherUser, "Drucker",
                LocalDate.of(2026, 3, 1), new BigDecimal("300.00"), 60, new BigDecimal("60.00")));

        List<DepreciationAsset> userAssets = depreciationAssetRepository.findByUserId(user.getId());
        assertThat(userAssets).hasSize(2);
    }

    @Test
    void findByExpenseEntryId_returnsLinkedAsset() {
        ExpenseEntry expense = expenseEntryRepository.save(new ExpenseEntry(user,
                new BigDecimal("1500.00"), "Arbeitsmittel", LocalDate.of(2026, 1, 15)));

        depreciationAssetRepository.save(new DepreciationAsset(user, "Laptop",
                LocalDate.of(2026, 1, 15), new BigDecimal("1500.00"),
                36, new BigDecimal("500.00"), expense));

        List<DepreciationAsset> linked = depreciationAssetRepository
                .findByExpenseEntryId(expense.getId());
        assertThat(linked).hasSize(1);
        assertThat(linked.getFirst().getName()).isEqualTo("Laptop");
    }

    @Test
    void sumAnnualAfaCentsByUserId_returnsCorrectTotal() {
        depreciationAssetRepository.save(new DepreciationAsset(user, "Laptop",
                LocalDate.of(2026, 1, 1), new BigDecimal("1500.00"), 36, new BigDecimal("500.00")));
        depreciationAssetRepository.save(new DepreciationAsset(user, "Monitor",
                LocalDate.of(2026, 2, 1), new BigDecimal("600.00"), 36, new BigDecimal("200.00")));
        depreciationAssetRepository.flush();

        Long totalCents = depreciationAssetRepository.sumAnnualAfaCentsByUserId(user.getId());

        // 500.00 + 200.00 = 700.00 EUR = 70000 cents
        assertThat(totalCents).isEqualTo(70000L);
    }

    @Test
    void sumAnnualAfaCentsByUserId_returnsNull_whenNoAssets() {
        Long totalCents = depreciationAssetRepository.sumAnnualAfaCentsByUserId(user.getId());
        assertThat(totalCents).isNull();
    }
}
