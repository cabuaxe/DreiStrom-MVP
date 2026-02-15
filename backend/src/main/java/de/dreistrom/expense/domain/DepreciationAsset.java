package de.dreistrom.expense.domain;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.domain.MoneyConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "depreciation_asset")
@Getter
@NoArgsConstructor
public class DepreciationAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false)
    private String name;

    @Column(name = "acquisition_date", nullable = false)
    private LocalDate acquisitionDate;

    @Convert(converter = MoneyConverter.class)
    @Column(name = "net_cost_cents", nullable = false)
    private BigDecimal netCost;

    @Column(name = "useful_life_months", nullable = false)
    private int usefulLifeMonths;

    @Convert(converter = MoneyConverter.class)
    @Column(name = "annual_afa_cents", nullable = false)
    private BigDecimal annualAfa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_entry_id")
    private ExpenseEntry expenseEntry;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private Instant updatedAt;

    public DepreciationAsset(AppUser user, String name, LocalDate acquisitionDate,
                             BigDecimal netCost, int usefulLifeMonths, BigDecimal annualAfa) {
        this.user = user;
        this.name = name;
        this.acquisitionDate = acquisitionDate;
        this.netCost = netCost;
        this.usefulLifeMonths = usefulLifeMonths;
        this.annualAfa = annualAfa;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public DepreciationAsset(AppUser user, String name, LocalDate acquisitionDate,
                             BigDecimal netCost, int usefulLifeMonths, BigDecimal annualAfa,
                             ExpenseEntry expenseEntry) {
        this(user, name, acquisitionDate, netCost, usefulLifeMonths, annualAfa);
        this.expenseEntry = expenseEntry;
    }

    public void update(String name, LocalDate acquisitionDate, BigDecimal netCost,
                       int usefulLifeMonths, BigDecimal annualAfa, ExpenseEntry expenseEntry) {
        this.name = name;
        this.acquisitionDate = acquisitionDate;
        this.netCost = netCost;
        this.usefulLifeMonths = usefulLifeMonths;
        this.annualAfa = annualAfa;
        this.expenseEntry = expenseEntry;
        this.updatedAt = Instant.now();
    }
}
