package de.dreistrom.vat.domain;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.domain.MoneyConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "vat_return")
@Getter
@NoArgsConstructor
public class VatReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "tax_period_id")
    private Long taxPeriodId;

    @Column(name = "`year`", nullable = false)
    private short year;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false)
    private PeriodType periodType;

    @Column(name = "period_number", nullable = false)
    private short periodNumber;

    @Convert(converter = MoneyConverter.class)
    @Column(name = "output_vat_cents", nullable = false)
    private BigDecimal outputVat;

    @Convert(converter = MoneyConverter.class)
    @Column(name = "input_vat_cents", nullable = false)
    private BigDecimal inputVat;

    @Convert(converter = MoneyConverter.class)
    @Column(name = "net_payable_cents", nullable = false)
    private BigDecimal netPayable;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VatReturnStatus status = VatReturnStatus.DRAFT;

    @Column(name = "submission_date")
    private LocalDate submissionDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private Instant updatedAt;

    public VatReturn(AppUser user, int year, PeriodType periodType, int periodNumber,
                     BigDecimal outputVat, BigDecimal inputVat, BigDecimal netPayable) {
        this.user = user;
        this.year = (short) year;
        this.periodType = periodType;
        this.periodNumber = (short) periodNumber;
        this.outputVat = outputVat;
        this.inputVat = inputVat;
        this.netPayable = netPayable;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void updateAmounts(BigDecimal outputVat, BigDecimal inputVat, BigDecimal netPayable) {
        this.outputVat = outputVat;
        this.inputVat = inputVat;
        this.netPayable = netPayable;
        this.updatedAt = Instant.now();
    }

    public void updateStatus(VatReturnStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public void submit(LocalDate submissionDate) {
        this.status = VatReturnStatus.SUBMITTED;
        this.submissionDate = submissionDate;
        this.updatedAt = Instant.now();
    }
}
