package de.dreistrom.calendar.domain;

import de.dreistrom.common.domain.AppUser;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ThresholdAlertTest {

    private final AppUser user = new AppUser("test@dreistrom.de", "hash", "Test User");

    @Test
    void createsAlertWithAutoCalculatedPercent() {
        var alert = new ThresholdAlert(user, ThresholdAlertType.KLEINUNTERNEHMER_CURRENT,
                new BigDecimal("18000"), new BigDecimal("25000"),
                "Kleinunternehmer-Grenze: 72% erreicht");

        assertThat(alert.getUser()).isEqualTo(user);
        assertThat(alert.getAlertType()).isEqualTo(ThresholdAlertType.KLEINUNTERNEHMER_CURRENT);
        assertThat(alert.getCurrentValue()).isEqualByComparingTo("18000");
        assertThat(alert.getThresholdValue()).isEqualByComparingTo("25000");
        assertThat(alert.getThresholdPercent()).isEqualByComparingTo("72.00");
        assertThat(alert.getMessage()).contains("72%");
        assertThat(alert.isAcknowledged()).isFalse();
        assertThat(alert.getTriggeredAt()).isNotNull();
    }

    @Test
    void handlesZeroThreshold() {
        var alert = new ThresholdAlert(user, ThresholdAlertType.CUSTOM,
                new BigDecimal("5000"), BigDecimal.ZERO,
                "Zero threshold alert");

        assertThat(alert.getThresholdPercent()).isEqualByComparingTo("0");
    }

    @Test
    void acknowledgesAlert() {
        var alert = new ThresholdAlert(user, ThresholdAlertType.ABFAERBUNG_RATIO,
                new BigDecimal("3500"), new BigDecimal("5000"),
                "Abfärbung-Verhältnis: 70%");

        alert.acknowledge();

        assertThat(alert.isAcknowledged()).isTrue();
        assertThat(alert.getAcknowledgedAt()).isNotNull();
    }

    @Test
    void calculatesPercentForGewstFreibetrag() {
        var alert = new ThresholdAlert(user, ThresholdAlertType.GEWST_FREIBETRAG,
                new BigDecimal("22000"), new BigDecimal("24500"),
                "GewSt-Freibetrag: 89,8% erreicht");

        assertThat(alert.getThresholdPercent()).isEqualByComparingTo("89.80");
    }

    @Test
    void calculatesPercentForReserveShortfall() {
        var alert = new ThresholdAlert(user, ThresholdAlertType.RESERVE_SHORTFALL,
                new BigDecimal("2400"), new BigDecimal("8000"),
                "Steuerrücklage nur 30% des Ziels");

        assertThat(alert.getThresholdPercent()).isEqualByComparingTo("30.00");
    }
}
