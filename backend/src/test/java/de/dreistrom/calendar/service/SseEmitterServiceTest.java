package de.dreistrom.calendar.service;

import de.dreistrom.common.sse.UnifiedSseEmitterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SseEmitterServiceTest {

    private UnifiedSseEmitterService sseService;

    @BeforeEach
    void setUp() {
        sseService = new UnifiedSseEmitterService();
    }

    @Test
    void subscribesAndReportsConnected() {
        assertThat(sseService.isConnected(1L)).isFalse();

        sseService.register(1L);

        assertThat(sseService.isConnected(1L)).isTrue();
    }

    @Test
    void sendDoesNotThrowWhenNotConnected() {
        // Should not throw
        sseService.send(99L, "notification", "test payload");
    }
}
