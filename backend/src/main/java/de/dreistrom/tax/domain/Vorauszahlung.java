package de.dreistrom.tax.domain;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.domain.MoneyConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "vorauszahlung")
@Getter
@NoArgsConstructor
public class Vorauszahlung {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "`year`", nullable = false)
    private short year;

    @Column(nullable = false)
    private short quarter;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Convert(converter = MoneyConverter.class)
    @Column(name = "assessment_basis_cents", nullable = false)
    private BigDecimal assessmentBasis;

    @Convert(converter = MoneyConverter.class)
    @Column(name = "amount_cents", nullable = false)
    private BigDecimal amount;

    @Convert(converter = MoneyConverter.class)
    @Column(name = "paid_cents", nullable = false)
    private BigDecimal paid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VorauszahlungStatus status = VorauszahlungStatus.PENDING;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private Instant updatedAt;

    public Vorauszahlung(AppUser user, int year, int quarter, LocalDate dueDate,
                         BigDecimal assessmentBasis, BigDecimal amount) {
        this.user = user;
        this.year = (short) year;
        this.quarter = (short) quarter;
        this.dueDate = dueDate;
        this.assessmentBasis = assessmentBasis;
        this.amount = amount;
        this.paid = BigDecimal.ZERO;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void markPaid(BigDecimal paidAmount, LocalDate paidDate) {
        this.paid = paidAmount;
        this.paidDate = paidDate;
        this.status = VorauszahlungStatus.PAID;
        this.updatedAt = Instant.now();
    }

    public void markOverdue() {
        this.status = VorauszahlungStatus.OVERDUE;
        this.updatedAt = Instant.now();
    }

    public void updateAmount(BigDecimal newAmount) {
        this.amount = newAmount;
        this.updatedAt = Instant.now();
    }

    public void updateAssessmentBasis(BigDecimal basis) {
        this.assessmentBasis = basis;
        this.updatedAt = Instant.now();
    }
}
