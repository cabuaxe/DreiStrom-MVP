package de.dreistrom.tax.controller;

import de.dreistrom.common.service.AppUserDetails;
import de.dreistrom.tax.dto.TaxReserveRecommendation;
import de.dreistrom.tax.service.TaxReserveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/tax/reserve")
@RequiredArgsConstructor
@Tag(name = "Tax Reserve", description = "Monthly tax reserve recommendations")
public class TaxReserveController {

    private final TaxReserveService taxReserveService;

    @GetMapping
    @Operation(summary = "Get tax reserve recommendation",
            description = "Calculates recommended monthly transfer to tax reserve account based on self-employed net profit",
            responses = @ApiResponse(responseCode = "200", description = "Reserve recommendation"))
    public ResponseEntity<TaxReserveRecommendation> getRecommendation(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "0") BigDecimal alreadyReserved) {

        int effectiveYear = year != null ? year : LocalDate.now().getYear();
        TaxReserveRecommendation recommendation = taxReserveService.calculate(
                userDetails.getId(), effectiveYear, alreadyReserved);
        return ResponseEntity.ok(recommendation);
    }
}
