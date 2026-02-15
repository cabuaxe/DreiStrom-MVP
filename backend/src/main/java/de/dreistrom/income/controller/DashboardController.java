package de.dreistrom.income.controller;

import de.dreistrom.common.service.AppUserDetails;
import de.dreistrom.income.dto.AbfaerbungStatusResponse;
import de.dreistrom.income.dto.ArbZGResponse;
import de.dreistrom.income.dto.GewerbesteuerThresholdResponse;
import de.dreistrom.income.dto.MandatoryFilingResponse;
import de.dreistrom.common.sse.UnifiedSseEmitterService;
import de.dreistrom.income.service.ArbZGService;
import de.dreistrom.income.service.DashboardService;
import de.dreistrom.income.service.GewerbesteuerThresholdService;
import de.dreistrom.income.service.MandatoryFilingService;
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
@Tag(name = "Dashboard", description = "Threshold monitoring dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final GewerbesteuerThresholdService gewerbesteuerThresholdService;
    private final MandatoryFilingService mandatoryFilingService;
    private final ArbZGService arbZGService;
    private final UnifiedSseEmitterService sseEmitterService;

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

    @GetMapping("/gewerbesteuer")
    @Operation(summary = "Get Gewerbesteuer threshold status (Freibetrag + Bilanzierungspflicht)",
            responses = @ApiResponse(responseCode = "200", description = "Gewerbesteuer threshold status"))
    public ResponseEntity<GewerbesteuerThresholdResponse> getGewerbesteuerThreshold(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @RequestParam(required = false) Integer year) {
        int effectiveYear = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(
                gewerbesteuerThresholdService.getStatus(userDetails.getId(), effectiveYear));
    }

    @GetMapping("/mandatory-filing")
    @Operation(summary = "Get mandatory tax filing status (§46 EStG, €410 threshold)",
            responses = @ApiResponse(responseCode = "200", description = "Mandatory filing status"))
    public ResponseEntity<MandatoryFilingResponse> getMandatoryFilingStatus(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @RequestParam(required = false) Integer year) {
        int effectiveYear = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(
                mandatoryFilingService.getStatus(userDetails.getId(), effectiveYear));
    }

    @GetMapping("/arbzg")
    @Operation(summary = "Get ArbZG §3 working time compliance (48h/week limit)",
            responses = @ApiResponse(responseCode = "200", description = "ArbZG compliance status"))
    public ResponseEntity<ArbZGResponse> getArbZGStatus(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @RequestParam(required = false) Integer year) {
        int effectiveYear = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(
                arbZGService.getStatus(userDetails.getId(), effectiveYear));
    }

    @GetMapping(path = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Subscribe to dashboard events via SSE",
            responses = @ApiResponse(responseCode = "200", description = "SSE event stream"))
    public SseEmitter subscribe(@AuthenticationPrincipal AppUserDetails userDetails) {
        return sseEmitterService.register(userDetails.getId());
    }
}
