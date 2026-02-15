package de.dreistrom.common.sse;

import de.dreistrom.common.service.AppUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Unified SSE event stream endpoint.
 * All real-time events (threshold alerts, notifications, feature flags)
 * are delivered through this single connection per client.
 */
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Unified Server-Sent Events stream")
public class EventStreamController {

    private final UnifiedSseEmitterService sseEmitterService;

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Subscribe to the unified event stream via SSE",
            description = "Delivers all real-time events: threshold alerts, notifications, and feature flags. "
                    + "Events use named types (e.g., 'kleinunternehmer', 'notification', 'feature-flags').",
            responses = @ApiResponse(responseCode = "200", description = "SSE event stream"))
    public SseEmitter subscribe(@AuthenticationPrincipal AppUserDetails userDetails) {
        return sseEmitterService.register(userDetails.getId());
    }
}
