package de.dreistrom.income.controller;

import de.dreistrom.common.service.AppUserDetails;
import de.dreistrom.income.dto.AbfaerbungStatusResponse;
import de.dreistrom.income.service.DashboardService;
import de.dreistrom.income.service.SseEmitterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Dashboard with Abfärbung monitoring")
public class DashboardController {

    private final DashboardService dashboardService;
    private final SseEmitterService sseEmitterService;

    @GetMapping("/abfaerbung")
    @Operation(summary = "Get current Abfärbung status for the authenticated user",
            responses = @ApiResponse(responseCode = "200", description = "Abfärbung status"))
    public ResponseEntity<AbfaerbungStatusResponse> getAbfaerbungStatus(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @RequestParam(required = false) Integer year) {
        int effectiveYear = year != null ? year : LocalDate.now().getYear();
        AbfaerbungStatusResponse status = dashboardService.getAbfaerbungStatus(
                userDetails.getId(), effectiveYear);
        return ResponseEntity.ok(status);
    }

    @GetMapping(path = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Subscribe to dashboard events via SSE",
            responses = @ApiResponse(responseCode = "200", description = "SSE event stream"))
    public SseEmitter subscribe(@AuthenticationPrincipal AppUserDetails userDetails) {
        return sseEmitterService.register(userDetails.getId());
    }
}
