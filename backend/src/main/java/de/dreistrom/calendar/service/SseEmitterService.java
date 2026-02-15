package de.dreistrom.calendar.service;

import de.dreistrom.calendar.domain.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages Server-Sent Event (SSE) connections for real-time in-app notifications.
 */
@Service
@Slf4j
public class SseEmitterService {

    private static final long SSE_TIMEOUT = 300_000L; // 5 minutes

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * Register a new SSE connection for a user.
     */
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        emitters.put(userId, emitter);

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> emitters.remove(userId));
        emitter.onError(e -> emitters.remove(userId));

        return emitter;
    }

    /**
     * Send a notification to a connected user.
     */
    public void sendToUser(Long userId, Notification notification) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name("notification")
                    .data(Map.of(
                            "id", notification.getId() != null ? notification.getId() : 0,
                            "title", notification.getTitle(),
                            "message", notification.getMessage(),
                            "daysBefore", notification.getDaysBefore()
                    )));
        } catch (IOException e) {
            log.debug("SSE send failed for user {}, removing emitter", userId);
            emitters.remove(userId);
        }
    }

    /**
     * Check if a user has an active SSE connection.
     */
    public boolean isConnected(Long userId) {
        return emitters.containsKey(userId);
    }
}
