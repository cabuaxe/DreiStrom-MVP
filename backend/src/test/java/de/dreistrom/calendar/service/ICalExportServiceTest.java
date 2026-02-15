package de.dreistrom.calendar.service;

import de.dreistrom.calendar.domain.ComplianceEvent;
import de.dreistrom.calendar.domain.ComplianceEventType;
import de.dreistrom.common.domain.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ICalExportServiceTest {

    private ICalExportService exportService;
    private final AppUser user = new AppUser("test@dreistrom.de", "hash", "Test User");

    @BeforeEach
    void setUp() {
        exportService = new ICalExportService();
    }

    @Test
    void exportsValidICalFormat() {
        var event = new ComplianceEvent(user, ComplianceEventType.UST_VA,
                "USt-VA 01/2026", "Umsatzsteuer-Voranmeldung Januar",
                LocalDate.of(2026, 2, 10), null, null);

        String ical = exportService.exportAsString(List.of(event));

        assertThat(ical).contains("BEGIN:VCALENDAR");
        assertThat(ical).contains("END:VCALENDAR");
        assertThat(ical).contains("PRODID:-//DreiStrom//Compliance Calendar//DE");
        assertThat(ical).contains("VERSION:2.0");
    }

    @Test
    void containsEventDetails() {
        var event = new ComplianceEvent(user, ComplianceEventType.EST_VORAUSZAHLUNG,
                "ESt-Vorauszahlung Q1/2026", "Einkommensteuer-Vorauszahlung Q1",
                LocalDate.of(2026, 3, 10), null, null);

        String ical = exportService.exportAsString(List.of(event));

        assertThat(ical).contains("BEGIN:VEVENT");
        assertThat(ical).contains("END:VEVENT");
        assertThat(ical).contains("SUMMARY:ESt-Vorauszahlung Q1/2026");
        assertThat(ical).contains("DESCRIPTION:Einkommensteuer-Vorauszahlung Q1");
    }

    @Test
    void containsAlarmReminders() {
        var event = new ComplianceEvent(user, ComplianceEventType.EUER_FILING,
                "EÜR 2025", "EÜR Filing",
                LocalDate.of(2026, 7, 31), null, null);

        String ical = exportService.exportAsString(List.of(event));

        assertThat(ical).contains("BEGIN:VALARM");
        assertThat(ical).contains("ACTION:DISPLAY");
    }

    @Test
    void exportsMultipleEvents() {
        var event1 = new ComplianceEvent(user, ComplianceEventType.UST_VA,
                "USt-VA 01/2026", null,
                LocalDate.of(2026, 2, 10), null, null);
        var event2 = new ComplianceEvent(user, ComplianceEventType.EST_VORAUSZAHLUNG,
                "ESt Q1/2026", null,
                LocalDate.of(2026, 3, 10), null, null);

        String ical = exportService.exportAsString(List.of(event1, event2));

        long eventCount = ical.lines().filter(l -> l.equals("BEGIN:VEVENT")).count();
        assertThat(eventCount).isEqualTo(2);
    }

    @Test
    void handlesEmptyList() {
        String ical = exportService.exportAsString(List.of());

        assertThat(ical).contains("BEGIN:VCALENDAR");
        assertThat(ical).contains("END:VCALENDAR");
        assertThat(ical).doesNotContain("BEGIN:VEVENT");
    }

    @Test
    void containsCategories() {
        var event = new ComplianceEvent(user, ComplianceEventType.ZM_REPORT,
                "ZM Q1/2026", null,
                LocalDate.of(2026, 4, 25), null, null);

        String ical = exportService.exportAsString(List.of(event));

        assertThat(ical).contains("CATEGORIES:ZM-Meldung");
    }
}
