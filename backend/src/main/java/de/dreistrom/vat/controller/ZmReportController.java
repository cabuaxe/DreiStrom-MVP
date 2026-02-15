package de.dreistrom.vat.controller;

import de.dreistrom.common.service.AppUserDetails;
import de.dreistrom.vat.dto.ZmReport;
import de.dreistrom.vat.service.ZmReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/tax/zm-report")
@RequiredArgsConstructor
@Tag(name = "ZM Report", description = "Zusammenfassende Meldung (§18a UStG) for EU B2B reverse charge")
public class ZmReportController {

    private final ZmReportService zmReportService;

    @GetMapping
    @Operation(summary = "Generate ZM report for a reporting period",
            description = "Aggregates EU B2B reverse charge invoices (including App Store payouts) "
                    + "by country and USt-IdNr for Zusammenfassende Meldung filing.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "ZM report generated"),
                    @ApiResponse(responseCode = "400", description = "Invalid date range")
            })
    public ResponseEntity<ZmReport> generate(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        ZmReport report = zmReportService.generate(userDetails.getId(), from, to);
        return ResponseEntity.ok(report);
    }
}
