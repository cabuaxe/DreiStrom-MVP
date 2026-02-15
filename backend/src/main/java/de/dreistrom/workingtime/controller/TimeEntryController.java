package de.dreistrom.workingtime.controller;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.workingtime.domain.ActivityType;
import de.dreistrom.workingtime.dto.TimeEntryResponse;
import de.dreistrom.workingtime.dto.WeeklySummary;
import de.dreistrom.workingtime.service.TimeEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/time-entries")
@RequiredArgsConstructor
public class TimeEntryController {

    private final TimeEntryService timeEntryService;

    @GetMapping
    public List<TimeEntryResponse> getEntries(
            @AuthenticationPrincipal AppUser user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return timeEntryService.getEntriesForRange(user.getId(), from, to).stream()
                .map(TimeEntryResponse::from)
                .toList();
    }

    @GetMapping("/weekly")
    public List<WeeklySummary> getWeeklySummaries(
            @AuthenticationPrincipal AppUser user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return timeEntryService.getWeeklySummaries(user.getId(), from, to);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TimeEntryResponse create(
            @AuthenticationPrincipal AppUser user,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam BigDecimal hours,
            @RequestParam ActivityType activityType,
            @RequestParam(required = false) String description) {
        return TimeEntryResponse.from(
                timeEntryService.create(user, date, hours, activityType, description));
    }

    @PutMapping("/{id}")
    public TimeEntryResponse update(
            @PathVariable Long id,
            @RequestParam BigDecimal hours,
            @RequestParam(required = false) String description) {
        return TimeEntryResponse.from(timeEntryService.update(id, hours, description));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        timeEntryService.delete(id);
    }
}
