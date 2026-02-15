package de.dreistrom.calendar.controller;

import de.dreistrom.calendar.domain.ComplianceEvent;
import de.dreistrom.calendar.dto.ComplianceEventResponse;
import de.dreistrom.calendar.service.ComplianceCalendarService;
import de.dreistrom.calendar.service.ICalExportService;
import de.dreistrom.common.domain.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/calendar")
@RequiredArgsConstructor
public class ComplianceCalendarController {

    private final ComplianceCalendarService calendarService;
    private final ICalExportService iCalExportService;

    @GetMapping("/events")
    public List<ComplianceEventResponse> getEvents(
            @AuthenticationPrincipal AppUser user,
            @RequestParam int year) {
        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year, 12, 31);
        return calendarService.getEvents(user.getId(), from, to).stream()
                .map(ComplianceEventResponse::from)
                .toList();
    }

    @GetMapping("/upcoming")
    public List<ComplianceEventResponse> getUpcoming(
            @AuthenticationPrincipal AppUser user) {
        return calendarService.getUpcomingEvents(user.getId()).stream()
                .map(ComplianceEventResponse::from)
                .toList();
    }

    @PostMapping("/generate")
    public List<ComplianceEventResponse> generateYear(
            @AuthenticationPrincipal AppUser user,
            @RequestParam int year) {
        return calendarService.generateYearEvents(user, year).stream()
                .map(ComplianceEventResponse::from)
                .toList();
    }

    @PostMapping("/events/{id}/complete")
    public ComplianceEventResponse completeEvent(@PathVariable Long id) {
        return ComplianceEventResponse.from(calendarService.completeEvent(id));
    }

    @GetMapping(value = "/export.ics", produces = "text/calendar")
    public ResponseEntity<String> exportICal(
            @AuthenticationPrincipal AppUser user,
            @RequestParam int year) {
        LocalDate from = LocalDate.of(year, 1, 1);
        LocalDate to = LocalDate.of(year, 12, 31);
        List<ComplianceEvent> events = calendarService.getEvents(user.getId(), from, to);
        String ical = iCalExportService.exportAsString(events);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"dreistrom-kalender-" + year + ".ics\"")
                .contentType(MediaType.parseMediaType("text/calendar"))
                .body(ical);
    }
}
