package de.dreistrom.vat.controller;

import de.dreistrom.common.service.AppUserDetails;
import de.dreistrom.vat.domain.PeriodType;
import de.dreistrom.vat.domain.VatReturn;
import de.dreistrom.vat.dto.KleinunternehmerStatus;
import de.dreistrom.vat.dto.VatReturnResponse;
import de.dreistrom.vat.service.KleinunternehmerStatusService;
import de.dreistrom.vat.service.VatReturnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/vat")
@RequiredArgsConstructor
@Tag(name = "VAT Returns", description = "Umsatzsteuervoranmeldung management")
public class VatReturnController {

    private final VatReturnService vatReturnService;
    private final KleinunternehmerStatusService kleinunternehmerStatusService;

    @GetMapping("/returns")
    @Operation(summary = "List VAT returns by year",
            responses = @ApiResponse(responseCode = "200", description = "VAT returns"))
    public ResponseEntity<List<VatReturnResponse>> listByYear(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @RequestParam int year,
            @RequestParam(required = false) PeriodType periodType) {

        List<VatReturn> returns = periodType != null
                ? vatReturnService.listByYearAndType(userDetails.getId(), year, periodType)
                : vatReturnService.listByYear(userDetails.getId(), year);

        return ResponseEntity.ok(returns.stream().map(this::toResponse).toList());
    }

    @GetMapping("/returns/{id}")
    @Operation(summary = "Get a VAT return by ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "VAT return found"),
                    @ApiResponse(responseCode = "404", description = "Not found")
            })
    public ResponseEntity<VatReturnResponse> getById(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable Long id) {

        VatReturn vr = vatReturnService.getById(id, userDetails.getId());
        return ResponseEntity.ok(toResponse(vr));
    }

    @PostMapping("/returns/{id}/submit")
    @Operation(summary = "Submit a DRAFT VAT return",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Submitted"),
                    @ApiResponse(responseCode = "400", description = "Not in DRAFT status")
            })
    public ResponseEntity<VatReturnResponse> submit(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable Long id) {

        VatReturn vr = vatReturnService.submit(id, userDetails.getId(), LocalDate.now());
        return ResponseEntity.ok(toResponse(vr));
    }

    @GetMapping("/kleinunternehmer")
    @Operation(summary = "Get Kleinunternehmer (§19 UStG) threshold status",
            responses = @ApiResponse(responseCode = "200", description = "Threshold status"))
    public ResponseEntity<KleinunternehmerStatus> getKleinunternehmerStatus(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @RequestParam(required = false) Integer year) {

        int effectiveYear = year != null ? year : LocalDate.now().getYear();
        KleinunternehmerStatus status = kleinunternehmerStatusService.getStatus(
                userDetails.getId(), effectiveYear);
        return ResponseEntity.ok(status);
    }

    private VatReturnResponse toResponse(VatReturn vr) {
        return new VatReturnResponse(
                vr.getId(),
                vr.getYear(),
                vr.getPeriodType(),
                vr.getPeriodNumber(),
                vr.getOutputVat(),
                vr.getInputVat(),
                vr.getNetPayable(),
                vr.getStatus(),
                vr.getSubmissionDate(),
                vr.getCreatedAt(),
                vr.getUpdatedAt()
        );
    }
}
