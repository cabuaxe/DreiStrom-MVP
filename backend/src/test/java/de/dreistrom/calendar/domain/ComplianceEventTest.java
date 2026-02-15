package de.dreistrom.calendar.domain;

import de.dreistrom.common.domain.AppUser;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class ComplianceEventTest {

    private final AppUser user = new AppUser("test@dreistrom.de", "hash", "Test User");

    @Test
    void createsWithCorrectDefaults() {
        var event = new ComplianceEvent(user, ComplianceEventType.UST_VA,
                "USt-VA Januar 2026", LocalDate.of(2026, 2, 10));

        assertThat(event.getUser()).isEqualTo(user);
        assertThat(event.getEventType()).isEqualTo(ComplianceEventType.UST_VA);
        assertThat(event.getTitle()).isEqualTo("USt-VA Januar 2026");
        assertThat(event.getDueDate()).isEqualTo(LocalDate.of(2026, 2, 10));
        assertThat(event.getStatus()).isEqualTo(ComplianceEventStatus.UPCOMING);
        assertThat(event.getCompletedAt()).isNull();
        assertThat(event.getCreatedAt()).isNotNull();
    }

    @Test
    void createsWithFullConstructor() {
        String config = "{\"channels\":[\"EMAIL\"],\"daysBefore\":[7,3,1]}";
        var event = new ComplianceEvent(user, ComplianceEventType.EST_VORAUSZAHLUNG,
                "ESt-VA Q1", "Einkommensteuer-Vorauszahlung Q1 2026",
                LocalDate.of(2026, 3, 10), 42L, config);

        assertThat(event.getDescription()).isEqualTo("Einkommensteuer-Vorauszahlung Q1 2026");
        assertThat(event.getTaxPeriodId()).isEqualTo(42L);
        assertThat(event.getReminderConfig()).isEqualTo(config);
    }

    @Test
    void marksDue() {
        var event = new ComplianceEvent(user, ComplianceEventType.EUER_FILING,
                "EÜR 2025", LocalDate.of(2026, 7, 31));

        event.markDue();

        assertThat(event.getStatus()).isEqualTo(ComplianceEventStatus.DUE);
    }

    @Test
    void marksOverdue() {
        var event = new ComplianceEvent(user, ComplianceEventType.GEWST_DECLARATION,
                "GewSt 2025", LocalDate.of(2026, 7, 31));

        event.markOverdue();

        assertThat(event.getStatus()).isEqualTo(ComplianceEventStatus.OVERDUE);
    }

    @Test
    void marksCompleted() {
        var event = new ComplianceEvent(user, ComplianceEventType.UST_VA,
                "USt-VA März 2026", LocalDate.of(2026, 4, 10));

        event.markCompleted();

        assertThat(event.getStatus()).isEqualTo(ComplianceEventStatus.COMPLETED);
        assertThat(event.getCompletedAt()).isNotNull();
    }

    @Test
    void cancelsEvent() {
        var event = new ComplianceEvent(user, ComplianceEventType.CUSTOM,
                "Custom event", LocalDate.of(2026, 6, 1));

        event.cancel();

        assertThat(event.getStatus()).isEqualTo(ComplianceEventStatus.CANCELLED);
    }

    @Test
    void reschedulesEvent() {
        var event = new ComplianceEvent(user, ComplianceEventType.EST_DECLARATION,
                "ESt 2025", LocalDate.of(2026, 7, 31));
        event.markOverdue();

        event.reschedule(LocalDate.of(2026, 9, 30));

        assertThat(event.getStatus()).isEqualTo(ComplianceEventStatus.UPCOMING);
        assertThat(event.getDueDate()).isEqualTo(LocalDate.of(2026, 9, 30));
    }

    @Test
    void updatesReminderConfig() {
        var event = new ComplianceEvent(user, ComplianceEventType.ZM_REPORT,
                "ZM Q1", LocalDate.of(2026, 4, 25));

        event.updateReminderConfig("{\"channels\":[\"EMAIL\",\"PUSH\"],\"daysBefore\":[14,7]}");

        assertThat(event.getReminderConfig()).contains("PUSH");
    }
}
