package de.dreistrom.calendar.service;

import de.dreistrom.calendar.domain.*;
import de.dreistrom.calendar.repository.ComplianceEventRepository;
import de.dreistrom.common.domain.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Populates and manages recurring German tax compliance deadlines.
 * Covers all statutory obligations for a Freiberuf+Gewerbe dual-stream taxpayer.
 */
@Service
@RequiredArgsConstructor
public class ComplianceCalendarService {

    private final ComplianceEventRepository eventRepository;

    private static final String DEFAULT_REMINDER_CONFIG =
            "{\"channels\":[\"EMAIL\"],\"daysBefore\":[14,7,3,1]}";

    /**
     * Generate all recurring compliance events for a given tax year.
     * Idempotent: skips events if matching ones already exist for the user/year.
     */
    @Transactional
    public List<ComplianceEvent> generateYearEvents(AppUser user, int year) {
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        List<ComplianceEvent> existing = eventRepository
                .findByUserIdAndDueDateBetweenOrderByDueDateAsc(user.getId(), yearStart, yearEnd);
        if (!existing.isEmpty()) {
            return existing;
        }

        List<ComplianceEvent> events = new ArrayList<>();
        events.addAll(generateEstVorauszahlungen(user, year));
        events.addAll(generateGewstVorauszahlungen(user, year));
        events.addAll(generateUstVoranmeldungen(user, year));
        events.addAll(generateZmDeadlines(user, year));
        events.addAll(generateAnnualDeclarations(user, year));
        events.add(generateIhkFee(user, year));
        events.addAll(generateSocialInsuranceDeadlines(user, year));

        return eventRepository.saveAll(events);
    }

    /**
     * Update statuses: mark events as DUE (today) or OVERDUE (past due).
     */
    @Transactional
    public void updateStatuses() {
        LocalDate today = LocalDate.now();

        List<ComplianceEvent> dueToday = eventRepository
                .findByStatusAndDueDateEquals(ComplianceEventStatus.UPCOMING, today);
        dueToday.forEach(ComplianceEvent::markDue);

        List<ComplianceEvent> overdue = eventRepository
                .findByStatusAndDueDateBefore(ComplianceEventStatus.UPCOMING, today);
        overdue.forEach(ComplianceEvent::markOverdue);

        List<ComplianceEvent> stillOverdue = eventRepository
                .findByStatusAndDueDateBefore(ComplianceEventStatus.DUE, today);
        stillOverdue.forEach(ComplianceEvent::markOverdue);
    }

    /**
     * Get all events for a user in a date range.
     */
    @Transactional(readOnly = true)
    public List<ComplianceEvent> getEvents(Long userId, LocalDate from, LocalDate to) {
        return eventRepository.findByUserIdAndDueDateBetweenOrderByDueDateAsc(userId, from, to);
    }

    /**
     * Get upcoming/overdue events for a user.
     */
    @Transactional(readOnly = true)
    public List<ComplianceEvent> getUpcomingEvents(Long userId) {
        List<ComplianceEvent> events = new ArrayList<>();
        events.addAll(eventRepository.findByUserIdAndStatusOrderByDueDateAsc(
                userId, ComplianceEventStatus.UPCOMING));
        events.addAll(eventRepository.findByUserIdAndStatusOrderByDueDateAsc(
                userId, ComplianceEventStatus.DUE));
        events.addAll(eventRepository.findByUserIdAndStatusOrderByDueDateAsc(
                userId, ComplianceEventStatus.OVERDUE));
        events.sort((a, b) -> a.getDueDate().compareTo(b.getDueDate()));
        return events;
    }

    /**
     * Mark a specific event as completed.
     */
    @Transactional
    public ComplianceEvent completeEvent(Long eventId) {
        ComplianceEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found: " + eventId));
        event.markCompleted();
        return event;
    }

    // ── Deadline generators ─────────────────────────────────────

    /** ESt-Vorauszahlungen: 10 Mar, 10 Jun, 10 Sep, 10 Dec */
    private List<ComplianceEvent> generateEstVorauszahlungen(AppUser user, int year) {
        return List.of(
                createEvent(user, ComplianceEventType.EST_VORAUSZAHLUNG,
                        "ESt-Vorauszahlung Q1/" + year, "Einkommensteuer-Vorauszahlung 1. Quartal",
                        LocalDate.of(year, 3, 10)),
                createEvent(user, ComplianceEventType.EST_VORAUSZAHLUNG,
                        "ESt-Vorauszahlung Q2/" + year, "Einkommensteuer-Vorauszahlung 2. Quartal",
                        LocalDate.of(year, 6, 10)),
                createEvent(user, ComplianceEventType.EST_VORAUSZAHLUNG,
                        "ESt-Vorauszahlung Q3/" + year, "Einkommensteuer-Vorauszahlung 3. Quartal",
                        LocalDate.of(year, 9, 10)),
                createEvent(user, ComplianceEventType.EST_VORAUSZAHLUNG,
                        "ESt-Vorauszahlung Q4/" + year, "Einkommensteuer-Vorauszahlung 4. Quartal",
                        LocalDate.of(year, 12, 10))
        );
    }

    /** GewSt-Vorauszahlungen: 15 Feb, 15 May, 15 Aug, 15 Nov */
    private List<ComplianceEvent> generateGewstVorauszahlungen(AppUser user, int year) {
        return List.of(
                createEvent(user, ComplianceEventType.GEWST_VORAUSZAHLUNG,
                        "GewSt-Vorauszahlung Q1/" + year, "Gewerbesteuer-Vorauszahlung 1. Quartal",
                        LocalDate.of(year, 2, 15)),
                createEvent(user, ComplianceEventType.GEWST_VORAUSZAHLUNG,
                        "GewSt-Vorauszahlung Q2/" + year, "Gewerbesteuer-Vorauszahlung 2. Quartal",
                        LocalDate.of(year, 5, 15)),
                createEvent(user, ComplianceEventType.GEWST_VORAUSZAHLUNG,
                        "GewSt-Vorauszahlung Q3/" + year, "Gewerbesteuer-Vorauszahlung 3. Quartal",
                        LocalDate.of(year, 8, 15)),
                createEvent(user, ComplianceEventType.GEWST_VORAUSZAHLUNG,
                        "GewSt-Vorauszahlung Q4/" + year, "Gewerbesteuer-Vorauszahlung 4. Quartal",
                        LocalDate.of(year, 11, 15))
        );
    }

    /** Monthly USt-Voranmeldung: 10th of following month */
    private List<ComplianceEvent> generateUstVoranmeldungen(AppUser user, int year) {
        List<ComplianceEvent> events = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            LocalDate dueDate;
            if (month == 12) {
                dueDate = LocalDate.of(year + 1, 1, 10);
            } else {
                dueDate = LocalDate.of(year, month + 1, 10);
            }
            String monthName = String.format("%02d", month);
            events.add(createEvent(user, ComplianceEventType.UST_VA,
                    "USt-VA " + monthName + "/" + year,
                    "Umsatzsteuer-Voranmeldung " + monthName + "/" + year,
                    dueDate));
        }
        return events;
    }

    /** Zusammenfassende Meldung: quarterly, 25th of following month */
    private List<ComplianceEvent> generateZmDeadlines(AppUser user, int year) {
        return List.of(
                createEvent(user, ComplianceEventType.ZM_REPORT,
                        "ZM Q1/" + year, "Zusammenfassende Meldung 1. Quartal",
                        LocalDate.of(year, 4, 25)),
                createEvent(user, ComplianceEventType.ZM_REPORT,
                        "ZM Q2/" + year, "Zusammenfassende Meldung 2. Quartal",
                        LocalDate.of(year, 7, 25)),
                createEvent(user, ComplianceEventType.ZM_REPORT,
                        "ZM Q3/" + year, "Zusammenfassende Meldung 3. Quartal",
                        LocalDate.of(year, 10, 25)),
                createEvent(user, ComplianceEventType.ZM_REPORT,
                        "ZM Q4/" + year, "Zusammenfassende Meldung 4. Quartal",
                        LocalDate.of(year + 1, 1, 25))
        );
    }

    /** Annual declarations: ESt (31 Jul next year), USt (31 Jul), GewSt (31 Jul), EÜR (31 Jul) */
    private List<ComplianceEvent> generateAnnualDeclarations(AppUser user, int year) {
        LocalDate deadline = LocalDate.of(year + 1, 7, 31);
        return List.of(
                createEvent(user, ComplianceEventType.EST_DECLARATION,
                        "Einkommensteuererklärung " + year,
                        "Jährliche Einkommensteuererklärung für " + year,
                        deadline),
                createEvent(user, ComplianceEventType.UST_DECLARATION,
                        "Umsatzsteuererklärung " + year,
                        "Jährliche Umsatzsteuererklärung für " + year,
                        deadline),
                createEvent(user, ComplianceEventType.GEWST_DECLARATION,
                        "Gewerbesteuererklärung " + year,
                        "Jährliche Gewerbesteuererklärung für " + year,
                        deadline),
                createEvent(user, ComplianceEventType.EUER_FILING,
                        "EÜR " + year,
                        "Einnahmen-Überschuss-Rechnung für " + year,
                        deadline)
        );
    }

    /** IHK contribution: typically due 15 Feb */
    private ComplianceEvent generateIhkFee(AppUser user, int year) {
        return createEvent(user, ComplianceEventType.CUSTOM,
                "IHK-Beitrag " + year, "Industrie- und Handelskammer Jahresbeitrag",
                LocalDate.of(year, 2, 15));
    }

    /** Social insurance: monthly, last day of month for Künstlersozialkasse etc. */
    private List<ComplianceEvent> generateSocialInsuranceDeadlines(AppUser user, int year) {
        List<ComplianceEvent> events = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            LocalDate lastDay = LocalDate.of(year, month, 1).plusMonths(1).minusDays(1);
            events.add(createEvent(user, ComplianceEventType.SOCIAL_INSURANCE,
                    "SV-Beitrag " + String.format("%02d", month) + "/" + year,
                    "Sozialversicherungsbeitrag " + String.format("%02d", month) + "/" + year,
                    lastDay));
        }
        return events;
    }

    private ComplianceEvent createEvent(AppUser user, ComplianceEventType type,
                                        String title, String description, LocalDate dueDate) {
        return new ComplianceEvent(user, type, title, description, dueDate,
                null, DEFAULT_REMINDER_CONFIG);
    }
}
