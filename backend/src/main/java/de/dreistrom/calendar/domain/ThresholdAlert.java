package de.dreistrom.calendar.domain;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.domain.MoneyConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "threshold_alert")
@Getter
@NoArgsConstructor
public class ThresholdAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false)
    private ThresholdAlertType alertType;

    @Convert(converter = MoneyConverter.class)
    @Column(name = "current_value_cents", nullable = false)
    private BigDecimal currentValue;

    @Convert(converter = MoneyConverter.class)
    @Column(name = "threshold_value_cents", nullable = false)
    private BigDecimal thresholdValue;

    @Column(name = "threshold_percent", precision = 5, scale = 2)
    private BigDecimal thresholdPercent;

    @Column(name = "triggered_at", nullable = false)
    private Instant triggeredAt;

    @Column(nullable = false)
    private boolean acknowledged = false;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(length = 500)
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private Instant updatedAt;

    public ThresholdAlert(AppUser user, ThresholdAlertType alertType,
                          BigDecimal currentValue, BigDecimal thresholdValue,
                          String message) {
        this.user = user;
        this.alertType = alertType;
        this.currentValue = currentValue;
        this.thresholdValue = thresholdValue;
        this.thresholdPercent = thresholdValue.signum() == 0
                ? BigDecimal.ZERO
                : currentValue.multiply(new BigDecimal("100"))
                        .divide(thresholdValue, 2, java.math.RoundingMode.HALF_UP);
        this.message = message;
        this.triggeredAt = Instant.now();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void acknowledge() {
        this.acknowledged = true;
        this.acknowledgedAt = Instant.now();
        this.updatedAt = Instant.now();
    }
}
