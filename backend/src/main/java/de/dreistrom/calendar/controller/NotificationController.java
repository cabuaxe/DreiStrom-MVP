package de.dreistrom.calendar.controller;

import de.dreistrom.calendar.domain.Notification;
import de.dreistrom.calendar.domain.NotificationChannel;
import de.dreistrom.calendar.dto.NotificationResponse;
import de.dreistrom.calendar.repository.NotificationRepository;
import de.dreistrom.calendar.service.SseEmitterService;
import de.dreistrom.common.domain.AppUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final SseEmitterService sseEmitterService;

    @GetMapping
    public List<NotificationResponse> getNotifications(
            @AuthenticationPrincipal AppUser user) {
        return notificationRepository
                .findByUserIdAndChannelAndReadAtIsNullOrderByCreatedAtDesc(
                        user.getId(), NotificationChannel.IN_APP)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @GetMapping("/count")
    public Map<String, Long> getUnreadCount(@AuthenticationPrincipal AppUser user) {
        return Map.of("unread", notificationRepository.countByUserIdAndReadAtIsNull(user.getId()));
    }

    @PostMapping("/{id}/read")
    public void markAsRead(@PathVariable Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.markRead();
            notificationRepository.save(n);
        });
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal AppUser user) {
        return sseEmitterService.subscribe(user.getId());
    }
}
