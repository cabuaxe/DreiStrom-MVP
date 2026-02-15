package de.dreistrom.calendar.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.dreistrom.calendar.domain.*;
import de.dreistrom.calendar.repository.ComplianceEventRepository;
import de.dreistrom.calendar.repository.NotificationRepository;
import de.dreistrom.common.domain.AppUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates multi-channel reminders for compliance events.
 * Checks each event's reminder_config JSON and dispatches through configured channels.
 */
@Service
@Slf4j
public class ReminderService {

    private static final List<Integer> DEFAULT_DAYS_BEFORE = List.of(14, 7, 3, 1);
    private static final List<String> DEFAULT_CHANNELS = List.of("IN_APP", "EMAIL");

    private final ComplianceEventRepository eventRepository;
    private final NotificationRepository notificationRepository;
    private final List<ReminderChannel> channels;
    private final ComplianceCalendarService calendarService;
    private final ObjectMapper objectMapper;

    public ReminderService(ComplianceEventRepository eventRepository,
                           NotificationRepository notificationRepository,
                           List<ReminderChannel> channels,
                           ComplianceCalendarService calendarService,
                           ObjectMapper objectMapper) {
        this.eventRepository = eventRepository;
        this.notificationRepository = notificationRepository;
        this.channels = channels;
        this.calendarService = calendarService;
        this.objectMapper = objectMapper;
    }

    /**
     * Daily check: update statuses and send reminders for upcoming events.
     */
    @Transactional
    public void dailyCheck() {
        calendarService.updateStatuses();

        LocalDate today = LocalDate.now();
        for (int daysBefore : DEFAULT_DAYS_BEFORE) {
            LocalDate targetDate = today.plusDays(daysBefore);
            processRemindersForDate(targetDate, daysBefore);
        }
        // Also check events due today
        processRemindersForDate(today, 0);

        log.info("Daily reminder check completed for {}", today);
    }

    /**
     * Send reminders for events due on a specific date.
     */
    @Transactional
    public void processRemindersForDate(LocalDate dueDate, int daysBefore) {
        List<ComplianceEvent> events = eventRepository
                .findByStatusAndDueDateEquals(ComplianceEventStatus.UPCOMING, dueDate);

        // Also include DUE status for same-day reminders
        if (daysBefore == 0) {
            events.addAll(eventRepository.findByStatusAndDueDateEquals(
                    ComplianceEventStatus.DUE, dueDate));
        }

        for (ComplianceEvent event : events) {
            sendReminders(event, daysBefore);
        }
    }

    private void sendReminders(ComplianceEvent event, int daysBefore) {
        ReminderConfig config = parseConfig(event.getReminderConfig());

        if (!shouldRemind(config, daysBefore)) {
            return;
        }

        AppUser user = event.getUser();
        for (String channelName : config.channels()) {
            NotificationChannel nc = NotificationChannel.valueOf(channelName);

            // Skip if already sent for this event/channel/daysBefore combo
            if (!notificationRepository.findByComplianceEventIdAndChannelAndDaysBefore(
                    event.getId(), nc, daysBefore).isEmpty()) {
                continue;
            }

            channels.stream()
                    .filter(c -> c.channel() == nc)
                    .findFirst()
                    .ifPresent(c -> {
                        try {
                            c.send(user, event, daysBefore);
                        } catch (Exception e) {
                            log.warn("Failed to send {} reminder for event {}: {}",
                                    nc, event.getId(), e.getMessage());
                        }
                    });
        }
    }

    private boolean shouldRemind(ReminderConfig config, int daysBefore) {
        return config.daysBefore().contains(daysBefore) || daysBefore == 0;
    }

    private ReminderConfig parseConfig(String json) {
        if (json == null || json.isBlank()) {
            return new ReminderConfig(DEFAULT_CHANNELS, DEFAULT_DAYS_BEFORE);
        }
        try {
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});
            @SuppressWarnings("unchecked")
            List<String> channelList = (List<String>) map.getOrDefault("channels", DEFAULT_CHANNELS);
            @SuppressWarnings("unchecked")
            List<Integer> daysBeforeList = ((List<Number>) map.getOrDefault("daysBefore",
                    DEFAULT_DAYS_BEFORE)).stream().map(Number::intValue).toList();
            return new ReminderConfig(channelList, daysBeforeList);
        } catch (Exception e) {
            log.warn("Failed to parse reminder config: {}", e.getMessage());
            return new ReminderConfig(DEFAULT_CHANNELS, DEFAULT_DAYS_BEFORE);
        }
    }

    private record ReminderConfig(List<String> channels, List<Integer> daysBefore) {
        private static List<Integer> of(int... values) {
            return java.util.Arrays.stream(values).boxed().toList();
        }
    }
}
