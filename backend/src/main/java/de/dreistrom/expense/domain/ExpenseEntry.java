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
@Table(name = "expense_entry")
@Getter
@NoArgsConstructor
public class ExpenseEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Convert(converter = MoneyConverter.class)
    @Column(name = "amount_cents", nullable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "EUR";

    @Column(nullable = false, length = 100)
    private String category;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "receipt_doc_id")
    private Long receiptDocId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocation_rule_id")
    private AllocationRule allocationRule;

    @Column(length = 500)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private Instant updatedAt;

    public ExpenseEntry(AppUser user, BigDecimal amount, String category, LocalDate entryDate) {
        this.user = user;
        this.amount = amount;
        this.category = category;
        this.entryDate = entryDate;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public ExpenseEntry(AppUser user, BigDecimal amount, String category, LocalDate entryDate,
                        AllocationRule allocationRule, Long receiptDocId, String description) {
        this(user, amount, category, entryDate);
        this.allocationRule = allocationRule;
        this.receiptDocId = receiptDocId;
        this.description = description;
    }

    public void update(BigDecimal amount, String category, LocalDate entryDate,
                       AllocationRule allocationRule, Long receiptDocId, String description) {
        this.amount = amount;
        this.category = category;
        this.entryDate = entryDate;
        this.allocationRule = allocationRule;
        this.receiptDocId = receiptDocId;
        this.description = description;
        this.updatedAt = Instant.now();
    }
}
