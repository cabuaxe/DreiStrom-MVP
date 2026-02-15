package de.dreistrom.income.controller;

import de.dreistrom.common.service.AppUserDetails;
import de.dreistrom.income.dto.UserFeatureFlags;
import de.dreistrom.income.service.FeatureFlagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@Tag(name = "Feature Flags", description = "Progressive disclosure feature flags")
public class FeatureFlagController {

    private final FeatureFlagService featureFlagService;

    @GetMapping("/feature-flags")
    @Operation(summary = "Get progressive disclosure flags computed from user's business volume",
            responses = @ApiResponse(responseCode = "200", description = "Feature flags"))
    public ResponseEntity<UserFeatureFlags> getFeatureFlags(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @RequestParam(required = false) Integer year) {
        int effectiveYear = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(
                featureFlagService.getFlags(userDetails.getId(), effectiveYear));
    }
}
