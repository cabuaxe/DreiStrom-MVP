package de.dreistrom.socialinsurance.controller;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.repository.AppUserRepository;
import de.dreistrom.common.service.AppUserDetails;
import de.dreistrom.socialinsurance.dto.SocialInsuranceStatus;
import de.dreistrom.socialinsurance.service.SocialInsuranceMonitorService;
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
@RequestMapping("/api/v1/social-insurance")
@RequiredArgsConstructor
@Tag(name = "Social Insurance", description = "Social insurance classification monitoring")
public class SocialInsuranceController {

    private final SocialInsuranceMonitorService monitorService;
    private final AppUserRepository appUserRepository;

    @GetMapping("/status")
    @Operation(summary = "Get social insurance classification status (§5 Abs. 5 SGB V)",
            responses = @ApiResponse(responseCode = "200", description = "Social insurance status"))
    public ResponseEntity<SocialInsuranceStatus> getStatus(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @RequestParam(required = false) Integer year) {
        int effectiveYear = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(
                monitorService.getStatus(userDetails.getId(), effectiveYear));
    }

    @PostMapping("/entry")
    @Operation(summary = "Create or update a monthly social insurance entry")
    public ResponseEntity<Void> upsertEntry(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam BigDecimal employmentHours,
            @RequestParam BigDecimal selfEmployedHours,
            @RequestParam BigDecimal employmentIncome,
            @RequestParam BigDecimal selfEmployedIncome) {
        AppUser user = appUserRepository.getReferenceById(userDetails.getId());
        monitorService.upsertEntry(user, year, month,
                employmentHours, selfEmployedHours,
                employmentIncome, selfEmployedIncome);
        return ResponseEntity.ok().build();
    }
}
