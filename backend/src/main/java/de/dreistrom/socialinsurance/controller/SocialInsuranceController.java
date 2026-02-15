package de.dreistrom.socialinsurance.controller;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.socialinsurance.dto.SocialInsuranceStatus;
import de.dreistrom.socialinsurance.service.SocialInsuranceMonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/social-insurance")
@RequiredArgsConstructor
public class SocialInsuranceController {

    private final SocialInsuranceMonitorService monitorService;

    @GetMapping("/status")
    public SocialInsuranceStatus getStatus(
            @AuthenticationPrincipal AppUser user,
            @RequestParam int year) {
        return monitorService.getStatus(user.getId(), year);
    }

    @PostMapping("/entry")
    public void upsertEntry(
            @AuthenticationPrincipal AppUser user,
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam BigDecimal employmentHours,
            @RequestParam BigDecimal selfEmployedHours,
            @RequestParam BigDecimal employmentIncome,
            @RequestParam BigDecimal selfEmployedIncome) {
        monitorService.upsertEntry(user, year, month,
                employmentHours, selfEmployedHours,
                employmentIncome, selfEmployedIncome);
    }
}
