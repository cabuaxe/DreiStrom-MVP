package de.dreistrom.socialinsurance.domain;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.domain.MoneyConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "social_insurance_entry")
@Getter
@NoArgsConstructor
public class SocialInsuranceEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "`year`", nullable = false)
    private short year;

    @Column(name = "`month`", nullable = false)
    private short month;

    @Column(name = "employment_hours_weekly", nullable = false, precision = 5, scale = 1)
    private BigDecimal employmentHoursWeekly;

    @Column(name = "self_employed_hours_weekly", nullable = false, precision = 5, scale = 1)
    private BigDecimal selfEmployedHoursWeekly;

    @Convert(converter = MoneyConverter.class)
    @Column(name = "employment_income_cents", nullable = false)
    private BigDecimal employmentIncome;

    @Convert(converter = MoneyConverter.class)
    @Column(name = "self_employed_income_cents", nullable = false)
    private BigDecimal selfEmployedIncome;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private Instant updatedAt;

    public SocialInsuranceEntry(AppUser user, int year, int month,
                                BigDecimal employmentHoursWeekly, BigDecimal selfEmployedHoursWeekly,
                                BigDecimal employmentIncome, BigDecimal selfEmployedIncome) {
        this.user = user;
        this.year = (short) year;
        this.month = (short) month;
        this.employmentHoursWeekly = employmentHoursWeekly;
        this.selfEmployedHoursWeekly = selfEmployedHoursWeekly;
        this.employmentIncome = employmentIncome;
        this.selfEmployedIncome = selfEmployedIncome;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void update(BigDecimal employmentHoursWeekly, BigDecimal selfEmployedHoursWeekly,
                       BigDecimal employmentIncome, BigDecimal selfEmployedIncome) {
        this.employmentHoursWeekly = employmentHoursWeekly;
        this.selfEmployedHoursWeekly = selfEmployedHoursWeekly;
        this.employmentIncome = employmentIncome;
        this.selfEmployedIncome = selfEmployedIncome;
        this.updatedAt = Instant.now();
    }
}
