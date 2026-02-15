package de.dreistrom.vat.controller;

import de.dreistrom.common.service.AppUserDetails;
import de.dreistrom.vat.domain.VatReturn;
import de.dreistrom.vat.service.ElsterExportService;
import de.dreistrom.vat.service.VatReturnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/tax/export/elster/vat")
@RequiredArgsConstructor
@Tag(name = "VAT Export", description = "ELSTER XML and CSV export for Umsatzsteuervoranmeldung")
public class VatExportController {

    private final VatReturnService vatReturnService;
    private final ElsterExportService elsterExportService;

    @GetMapping("/{periodId}")
    @Operation(summary = "Export VAT return as ERiC-compatible ELSTER XML",
            responses = {
                    @ApiResponse(responseCode = "200", description = "ELSTER XML generated"),
                    @ApiResponse(responseCode = "404", description = "VAT return not found")
            })
    public ResponseEntity<byte[]> exportXml(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable Long periodId) {

        VatReturn vatReturn = vatReturnService.getById(periodId, userDetails.getId());
        byte[] xml = elsterExportService.generateElsterXml(vatReturn, userDetails.getDisplayName());

        String filename = String.format("UStVA_%d_%02d.xml",
                vatReturn.getYear(), vatReturn.getPeriodNumber());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.APPLICATION_XML)
                .body(xml);
    }

    @GetMapping("/{periodId}/csv")
    @Operation(summary = "Export VAT return as CSV for Steuerberater",
            responses = {
                    @ApiResponse(responseCode = "200", description = "CSV generated"),
                    @ApiResponse(responseCode = "404", description = "VAT return not found")
            })
    public ResponseEntity<byte[]> exportCsv(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable Long periodId) {

        VatReturn vatReturn = vatReturnService.getById(periodId, userDetails.getId());
        byte[] csv = elsterExportService.generateCsv(vatReturn);

        String filename = String.format("UStVA_%d_%02d.csv",
                vatReturn.getYear(), vatReturn.getPeriodNumber());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv);
    }
}
