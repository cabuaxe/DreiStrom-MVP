package de.dreistrom.calendar.service;

import de.dreistrom.calendar.domain.*;
import de.dreistrom.calendar.repository.ComplianceEventRepository;
import de.dreistrom.common.domain.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplianceCalendarServiceTest {

    @Mock
    private ComplianceEventRepository eventRepository;

    @InjectMocks
    private ComplianceCalendarService service;

    @Captor
    private ArgumentCaptor<List<ComplianceEvent>> eventsCaptor;

    private final AppUser user = new AppUser("test@dreistrom.de", "hash", "Test User");

    @Nested
    class GenerateYearEvents {

        @Test
        void generatesAllRecurringEventsFor2026() {
            when(eventRepository.findByUserIdAndDueDateBetweenOrderByDueDateAsc(
                    any(), any(), any())).thenReturn(Collections.emptyList());
            when(eventRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

            List<ComplianceEvent> events = service.generateYearEvents(user, 2026);

            verify(eventRepository).saveAll(eventsCaptor.capture());
            List<ComplianceEvent> saved = eventsCaptor.getValue();

            // 4 ESt-VA + 4 GewSt-VA + 12 USt-VA + 4 ZM + 4 annual + 1 IHK + 12 SV = 41
            assertThat(saved).hasSize(41);
        }

        @Test
        void generatesCorrectEstVorauszahlungDates() {
            when(eventRepository.findByUserIdAndDueDateBetweenOrderByDueDateAsc(
                    any(), any(), any())).thenReturn(Collections.emptyList());
            when(eventRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

            service.generateYearEvents(user, 2026);

            verify(eventRepository).saveAll(eventsCaptor.capture());
            List<ComplianceEvent> estEvents = eventsCaptor.getValue().stream()
                    .filter(e -> e.getEventType() == ComplianceEventType.EST_VORAUSZAHLUNG)
                    .toList();

            assertThat(estEvents).hasSize(4);
            assertThat(estEvents.get(0).getDueDate()).isEqualTo(LocalDate.of(2026, 3, 10));
            assertThat(estEvents.get(1).getDueDate()).isEqualTo(LocalDate.of(2026, 6, 10));
            assertThat(estEvents.get(2).getDueDate()).isEqualTo(LocalDate.of(2026, 9, 10));
            assertThat(estEvents.get(3).getDueDate()).isEqualTo(LocalDate.of(2026, 12, 10));
        }

        @Test
        void generatesCorrectGewstVorauszahlungDates() {
            when(eventRepository.findByUserIdAndDueDateBetweenOrderByDueDateAsc(
                    any(), any(), any())).thenReturn(Collections.emptyList());
            when(eventRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

            service.generateYearEvents(user, 2026);

            verify(eventRepository).saveAll(eventsCaptor.capture());
            List<ComplianceEvent> gewstEvents = eventsCaptor.getValue().stream()
                    .filter(e -> e.getEventType() == ComplianceEventType.GEWST_VORAUSZAHLUNG)
                    .toList();

            assertThat(gewstEvents).hasSize(4);
            assertThat(gewstEvents.get(0).getDueDate()).isEqualTo(LocalDate.of(2026, 2, 15));
            assertThat(gewstEvents.get(1).getDueDate()).isEqualTo(LocalDate.of(2026, 5, 15));
            assertThat(gewstEvents.get(2).getDueDate()).isEqualTo(LocalDate.of(2026, 8, 15));
            assertThat(gewstEvents.get(3).getDueDate()).isEqualTo(LocalDate.of(2026, 11, 15));
        }

        @Test
        void generatesMonthlyUstVoranmeldungen() {
            when(eventRepository.findByUserIdAndDueDateBetweenOrderByDueDateAsc(
                    any(), any(), any())).thenReturn(Collections.emptyList());
            when(eventRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

            service.generateYearEvents(user, 2026);

            verify(eventRepository).saveAll(eventsCaptor.capture());
            List<ComplianceEvent> ustEvents = eventsCaptor.getValue().stream()
                    .filter(e -> e.getEventType() == ComplianceEventType.UST_VA)
                    .toList();

            assertThat(ustEvents).hasSize(12);
            // January USt-VA due 10 Feb
            assertThat(ustEvents.get(0).getDueDate()).isEqualTo(LocalDate.of(2026, 2, 10));
            assertThat(ustEvents.get(0).getTitle()).isEqualTo("USt-VA 01/2026");
            // December USt-VA due 10 Jan next year
            assertThat(ustEvents.get(11).getDueDate()).isEqualTo(LocalDate.of(2027, 1, 10));
        }

        @Test
        void generatesAnnualDeclarationsWithJulyDeadline() {
            when(eventRepository.findByUserIdAndDueDateBetweenOrderByDueDateAsc(
                    any(), any(), any())).thenReturn(Collections.emptyList());
            when(eventRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

            service.generateYearEvents(user, 2025);

            verify(eventRepository).saveAll(eventsCaptor.capture());
            List<ComplianceEvent> annualEvents = eventsCaptor.getValue().stream()
                    .filter(e -> e.getEventType() == ComplianceEventType.EST_DECLARATION
                            || e.getEventType() == ComplianceEventType.UST_DECLARATION
                            || e.getEventType() == ComplianceEventType.GEWST_DECLARATION
                            || e.getEventType() == ComplianceEventType.EUER_FILING)
                    .toList();

            assertThat(annualEvents).hasSize(4);
            // All due 31 Jul of next year
            annualEvents.forEach(e ->
                    assertThat(e.getDueDate()).isEqualTo(LocalDate.of(2026, 7, 31)));
        }

        @Test
        void skipsGenerationIfEventsAlreadyExist() {
            ComplianceEvent existing = new ComplianceEvent(user, ComplianceEventType.EST_VORAUSZAHLUNG,
                    "Existing", LocalDate.of(2026, 3, 10));
            when(eventRepository.findByUserIdAndDueDateBetweenOrderByDueDateAsc(
                    any(), any(), any())).thenReturn(List.of(existing));

            List<ComplianceEvent> result = service.generateYearEvents(user, 2026);

            verify(eventRepository, never()).saveAll(anyList());
            assertThat(result).hasSize(1);
        }

        @Test
        void includesReminderConfig() {
            when(eventRepository.findByUserIdAndDueDateBetweenOrderByDueDateAsc(
                    any(), any(), any())).thenReturn(Collections.emptyList());
            when(eventRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

            service.generateYearEvents(user, 2026);

            verify(eventRepository).saveAll(eventsCaptor.capture());
            eventsCaptor.getValue().forEach(e ->
                    assertThat(e.getReminderConfig()).contains("daysBefore"));
        }
    }

    @Nested
    class StatusUpdate {

        @Test
        void marksDueEventsForToday() {
            ComplianceEvent event = new ComplianceEvent(user, ComplianceEventType.UST_VA,
                    "USt-VA", LocalDate.now());
            when(eventRepository.findByStatusAndDueDateEquals(
                    ComplianceEventStatus.UPCOMING, LocalDate.now()))
                    .thenReturn(List.of(event));
            when(eventRepository.findByStatusAndDueDateBefore(any(), any()))
                    .thenReturn(Collections.emptyList());

            service.updateStatuses();

            assertThat(event.getStatus()).isEqualTo(ComplianceEventStatus.DUE);
        }

        @Test
        void marksOverdueEvents() {
            ComplianceEvent event = new ComplianceEvent(user, ComplianceEventType.EST_VORAUSZAHLUNG,
                    "ESt-VA Q1", LocalDate.now().minusDays(5));
            when(eventRepository.findByStatusAndDueDateEquals(any(), any()))
                    .thenReturn(Collections.emptyList());
            when(eventRepository.findByStatusAndDueDateBefore(
                    eq(ComplianceEventStatus.UPCOMING), any()))
                    .thenReturn(List.of(event));
            when(eventRepository.findByStatusAndDueDateBefore(
                    eq(ComplianceEventStatus.DUE), any()))
                    .thenReturn(Collections.emptyList());

            service.updateStatuses();

            assertThat(event.getStatus()).isEqualTo(ComplianceEventStatus.OVERDUE);
        }
    }

    @Nested
    class CompleteEvent {

        @Test
        void completesEventById() {
            ComplianceEvent event = new ComplianceEvent(user, ComplianceEventType.UST_VA,
                    "USt-VA 01/2026", LocalDate.of(2026, 2, 10));
            when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

            ComplianceEvent result = service.completeEvent(1L);

            assertThat(result.getStatus()).isEqualTo(ComplianceEventStatus.COMPLETED);
            assertThat(result.getCompletedAt()).isNotNull();
        }
    }
}
