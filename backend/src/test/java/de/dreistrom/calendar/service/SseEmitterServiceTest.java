package de.dreistrom.calendar.service;

import de.dreistrom.calendar.domain.ComplianceEvent;
import de.dreistrom.calendar.domain.ComplianceEventType;
import de.dreistrom.calendar.domain.Notification;
import de.dreistrom.calendar.domain.NotificationChannel;
import de.dreistrom.common.domain.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class SseEmitterServiceTest {

    private SseEmitterService sseService;
    private final AppUser user = new AppUser("test@dreistrom.de", "hash", "Test");

    @BeforeEach
    void setUp() {
        sseService = new SseEmitterService();
    }

    @Test
    void subscribesAndReportsConnected() {
        assertThat(sseService.isConnected(1L)).isFalse();

        sseService.subscribe(1L);

        assertThat(sseService.isConnected(1L)).isTrue();
    }

    @Test
    void sendDoesNotThrowWhenNotConnected() {
        var event = new ComplianceEvent(user, ComplianceEventType.UST_VA,
                "Test", LocalDate.now());
        var notification = new Notification(user, event, NotificationChannel.IN_APP,
                "Title", "Message", 7);

        // Should not throw
        sseService.sendToUser(99L, notification);
    }
}
