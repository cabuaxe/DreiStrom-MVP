package de.dreistrom.tax.controller;

import de.dreistrom.common.service.AppUserDetails;
import de.dreistrom.tax.dto.AnnualTaxPackage;
import de.dreistrom.tax.service.AnnualTaxExportService;
import de.dreistrom.tax.service.AnnualTaxPackageService;
import de.dreistrom.tax.service.AnnualTaxPdfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for the annual tax return package export.
 * Provides JSON, ELSTER XML, CSV, and PDF download endpoints.
 * <p>
 * Base path: /api/v1/tax/export/annual/{year}
 */
@RestController
@RequestMapping("/api/v1/tax/export/annual")
@RequiredArgsConstructor
@Tag(name = "Annual Tax Export",
        description = "Einkommensteuererklaerung export (Anlage N, S, G, EÜR, Vorsorgeaufwand)")
public class AnnualTaxExportController {

    private final AnnualTaxPackageService packageService;
    private final AnnualTaxExportService exportService;
    private final AnnualTaxPdfService pdfService;

    @GetMapping("/{year}")
    @Operation(summary = "Get annual tax package as JSON",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Annual tax package data"),
                    @ApiResponse(responseCode = "401", description = "Unauthorized")
            })
    public ResponseEntity<AnnualTaxPackage> getPackage(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable int year) {

        AnnualTaxPackage pkg = packageService.assemble(userDetails.getId(), year);
        return ResponseEntity.ok(pkg);
    }

    @GetMapping("/{year}/xml")
    @Operation(summary = "Download annual tax return as ELSTER XML",
            responses = @ApiResponse(responseCode = "200", description = "ELSTER XML document"))
    public ResponseEntity<byte[]> downloadXml(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable int year) {

        AnnualTaxPackage pkg = packageService.assemble(userDetails.getId(), year);
        byte[] xml = exportService.generateElsterXml(pkg, userDetails.getDisplayName());

        String filename = "ESt_" + year + "_ELSTER.xml";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_XML)
                .body(xml);
    }

    @GetMapping("/{year}/csv")
    @Operation(summary = "Download annual tax return as CSV",
            responses = @ApiResponse(responseCode = "200", description = "CSV document"))
    public ResponseEntity<byte[]> downloadCsv(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable int year) {

        AnnualTaxPackage pkg = packageService.assemble(userDetails.getId(), year);
        byte[] csv = exportService.generateCsv(pkg);

        String filename = "ESt_" + year + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping("/{year}/pdf")
    @Operation(summary = "Download annual tax return as PDF",
            responses = @ApiResponse(responseCode = "200", description = "PDF document"))
    public ResponseEntity<byte[]> downloadPdf(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable int year) {

        AnnualTaxPackage pkg = packageService.assemble(userDetails.getId(), year);
        byte[] pdf = pdfService.generate(pkg);

        String filename = "ESt_" + year + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
