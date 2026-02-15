package de.dreistrom.calendar.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.dreistrom.calendar.domain.*;
import de.dreistrom.calendar.repository.ComplianceEventRepository;
import de.dreistrom.calendar.repository.NotificationRepository;
import de.dreistrom.common.domain.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {

    @Mock private ComplianceEventRepository eventRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private ComplianceCalendarService calendarService;
    @Mock private InAppReminderChannel inAppChannel;
    @Mock private EmailReminderChannel emailChannel;

    private ReminderService reminderService;
    private final AppUser user = new AppUser("test@dreistrom.de", "hash", "Test User");
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        lenient().when(inAppChannel.channel()).thenReturn(NotificationChannel.IN_APP);
        lenient().when(emailChannel.channel()).thenReturn(NotificationChannel.EMAIL);
        reminderService = new ReminderService(
                eventRepository, notificationRepository,
                List.of(inAppChannel, emailChannel),
                calendarService, objectMapper);
    }

    @Test
    void dailyCheckUpdatesStatusesAndProcessesReminders() {
        when(eventRepository.findByStatusAndDueDateEquals(any(), any()))
                .thenReturn(Collections.emptyList());

        reminderService.dailyCheck();

        verify(calendarService).updateStatuses();
    }

    @Test
    void sendsInAppAndEmailForConfiguredChannels() {
        String config = "{\"channels\":[\"IN_APP\",\"EMAIL\"],\"daysBefore\":[7]}";
        ComplianceEvent event = new ComplianceEvent(user, ComplianceEventType.UST_VA,
                "USt-VA 01/2026", "Test", LocalDate.now().plusDays(7), null, config);

        when(eventRepository.findByStatusAndDueDateEquals(
                ComplianceEventStatus.UPCOMING, LocalDate.now().plusDays(7)))
                .thenReturn(new java.util.ArrayList<>(List.of(event)));
        when(notificationRepository.findByComplianceEventIdAndChannelAndDaysBefore(
                any(), any(), anyInt())).thenReturn(Collections.emptyList());

        reminderService.processRemindersForDate(LocalDate.now().plusDays(7), 7);

        verify(inAppChannel).send(user, event, 7);
        verify(emailChannel).send(user, event, 7);
    }

    @Test
    void skipsAlreadySentNotifications() {
        String config = "{\"channels\":[\"IN_APP\"],\"daysBefore\":[7]}";
        ComplianceEvent event = new ComplianceEvent(user, ComplianceEventType.UST_VA,
                "USt-VA", "Test", LocalDate.now().plusDays(7), null, config);
        Notification existing = new Notification(user, event, NotificationChannel.IN_APP,
                "title", "msg", 7);

        when(eventRepository.findByStatusAndDueDateEquals(
                ComplianceEventStatus.UPCOMING, LocalDate.now().plusDays(7)))
                .thenReturn(new java.util.ArrayList<>(List.of(event)));
        when(notificationRepository.findByComplianceEventIdAndChannelAndDaysBefore(
                any(), eq(NotificationChannel.IN_APP), eq(7)))
                .thenReturn(List.of(existing));

        reminderService.processRemindersForDate(LocalDate.now().plusDays(7), 7);

        verify(inAppChannel, never()).send(any(), any(), anyInt());
    }

    @Test
    void handlesNullReminderConfig() {
        ComplianceEvent event = new ComplianceEvent(user, ComplianceEventType.EST_VORAUSZAHLUNG,
                "ESt-VA Q1", LocalDate.now().plusDays(14));

        when(eventRepository.findByStatusAndDueDateEquals(
                ComplianceEventStatus.UPCOMING, LocalDate.now().plusDays(14)))
                .thenReturn(new java.util.ArrayList<>(List.of(event)));
        when(notificationRepository.findByComplianceEventIdAndChannelAndDaysBefore(
                any(), any(), anyInt())).thenReturn(Collections.emptyList());

        reminderService.processRemindersForDate(LocalDate.now().plusDays(14), 14);

        // Default config includes IN_APP and EMAIL
        verify(inAppChannel).send(user, event, 14);
        verify(emailChannel).send(user, event, 14);
    }

    @Test
    void processesZeroDayRemindersForDueEvents() {
        ComplianceEvent event = new ComplianceEvent(user, ComplianceEventType.EUER_FILING,
                "EÜR 2025", "Test", LocalDate.now(), null, null);
        event.markDue();

        when(eventRepository.findByStatusAndDueDateEquals(
                ComplianceEventStatus.UPCOMING, LocalDate.now()))
                .thenReturn(new java.util.ArrayList<>());
        when(eventRepository.findByStatusAndDueDateEquals(
                ComplianceEventStatus.DUE, LocalDate.now()))
                .thenReturn(new java.util.ArrayList<>(List.of(event)));
        when(notificationRepository.findByComplianceEventIdAndChannelAndDaysBefore(
                any(), any(), anyInt())).thenReturn(Collections.emptyList());

        reminderService.processRemindersForDate(LocalDate.now(), 0);

        verify(inAppChannel).send(user, event, 0);
    }
}
