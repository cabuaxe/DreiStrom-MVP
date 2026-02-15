package de.dreistrom.common.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Unified SSE emitter service for all real-time events across the application.
 * Replaces the module-specific IncomeSseEmitterService and calendar SseEmitterService.
 * <p>
 * Supports multiple concurrent connections per user, named events,
 * and periodic heartbeats for connection keepalive.
 */
@Slf4j
@Service
public class UnifiedSseEmitterService {

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /**
     * Register a new SSE connection for a user.
     * Sends an initial heartbeat to confirm the connection is alive.
     */
    public SseEmitter register(Long userId) {
        SseEmitter emitter = new SseEmitter(0L); // no timeout — managed by heartbeat
        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(e -> remove(userId, emitter));

        // Send initial heartbeat so client knows connection is established
        try {
            emitter.send(SseEmitter.event().comment("connected"));
        } catch (IOException e) {
            remove(userId, emitter);
        }

        return emitter;
    }

    /**
     * Send a default (unnamed) event to a user.
     */
    public void send(Long userId, Object data) {
        send(userId, null, data);
    }

    /**
     * Send a named event to a user. All connected emitters receive the event.
     *
     * @param userId    target user
     * @param eventName SSE event name (null for default message event)
     * @param data      payload, serialized as JSON
     */
    public void send(Long userId, String eventName, Object data) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null) {
            return;
        }
        for (SseEmitter emitter : userEmitters) {
            try {
                SseEmitter.SseEventBuilder builder = SseEmitter.event()
                        .data(data, MediaType.APPLICATION_JSON);
                if (eventName != null) {
                    builder.name(eventName);
                }
                emitter.send(builder);
            } catch (IOException e) {
                log.debug("Failed to send SSE event to user {}, removing emitter", userId);
                remove(userId, emitter);
            }
        }
    }

    /**
     * Check if a user has any active SSE connections.
     */
    public boolean isConnected(Long userId) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        return userEmitters != null && !userEmitters.isEmpty();
    }

    /**
     * Heartbeat: send a comment event every 30 seconds to keep connections alive
     * and detect stale emitters.
     */
    @Scheduled(fixedRate = 30_000)
    void heartbeat() {
        emitters.forEach((userId, userEmitters) -> {
            for (SseEmitter emitter : userEmitters) {
                try {
                    emitter.send(SseEmitter.event().comment("heartbeat"));
                } catch (IOException e) {
                    remove(userId, emitter);
                }
            }
        });
    }

    private void remove(Long userId, SseEmitter emitter) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters != null) {
            userEmitters.remove(emitter);
            if (userEmitters.isEmpty()) {
                emitters.remove(userId);
            }
        }
    }
}
