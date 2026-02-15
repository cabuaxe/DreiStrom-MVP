package de.dreistrom.tax.controller;

import de.dreistrom.common.domain.IncomeStream;
import de.dreistrom.common.service.AppUserDetails;
import de.dreistrom.tax.dto.EuerResult;
import de.dreistrom.tax.service.EuerPdfService;
import de.dreistrom.tax.service.EuerService;
import de.dreistrom.tax.service.EuerService.DualStreamEuer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bookkeeping/eur")
@RequiredArgsConstructor
@Tag(name = "EÜR", description = "Einnahmen-Überschuss-Rechnung per §4 Abs. 3 EStG")
public class EuerController {

    private final EuerService euerService;
    private final EuerPdfService euerPdfService;

    @GetMapping("/{stream}/{year}")
    @Operation(summary = "Get EÜR for a stream and year",
            responses = {
                    @ApiResponse(responseCode = "200", description = "EÜR data"),
                    @ApiResponse(responseCode = "400", description = "Invalid stream")
            })
    public ResponseEntity<EuerResult> getEuer(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable IncomeStream stream,
            @PathVariable int year) {

        EuerResult result = euerService.generate(userDetails.getId(), stream, year);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/dual/{year}")
    @Operation(summary = "Get dual-stream EÜR comparison for a year",
            responses = @ApiResponse(responseCode = "200", description = "Dual-stream EÜR"))
    public ResponseEntity<DualStreamEuer> getDualEuer(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable int year) {

        DualStreamEuer result = euerService.generateDual(userDetails.getId(), year);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{stream}/{year}/pdf")
    @Operation(summary = "Download single-stream EÜR as PDF",
            responses = @ApiResponse(responseCode = "200", description = "PDF document"))
    public ResponseEntity<byte[]> downloadPdf(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable IncomeStream stream,
            @PathVariable int year) {

        EuerResult euer = euerService.generate(userDetails.getId(), stream, year);
        byte[] pdf = euerPdfService.generateSingleStream(euer);

        String filename = "EUeR_" + stream.name() + "_" + year + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/dual/{year}/pdf")
    @Operation(summary = "Download dual-stream comparison EÜR as PDF",
            responses = @ApiResponse(responseCode = "200", description = "PDF document"))
    public ResponseEntity<byte[]> downloadDualPdf(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable int year) {

        DualStreamEuer dual = euerService.generateDual(userDetails.getId(), year);
        byte[] pdf = euerPdfService.generateDualStream(dual);

        String filename = "EUeR_Vergleich_" + year + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
