package de.dreistrom.onboarding.controller;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.service.AppUserDetails;
import de.dreistrom.onboarding.domain.DecisionChoice;
import de.dreistrom.onboarding.dto.*;
import de.dreistrom.onboarding.service.DecisionEngineService;
import de.dreistrom.onboarding.service.OnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
@Tag(name = "Onboarding", description = "15-step registration checklist management")
public class OnboardingController {

    private final OnboardingService onboardingService;
    private final DecisionEngineService decisionEngineService;
    private final EntityManager entityManager;

    @PostMapping("/initialize")
    @Operation(summary = "Initialize the 15-step onboarding checklist for the current user",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Checklist initialized or already exists")
            })
    public ResponseEntity<OnboardingProgressResponse> initialize(
            @AuthenticationPrincipal AppUserDetails userDetails) {

        AppUser user = entityManager.getReference(AppUser.class, userDetails.getId());
        onboardingService.initializeChecklist(user);
        return ResponseEntity.ok(onboardingService.getProgress(userDetails.getId()));
    }

    @GetMapping("/progress")
    @Operation(summary = "Get onboarding progress with all steps and decision points",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Progress retrieved")
            })
    public ResponseEntity<OnboardingProgressResponse> getProgress(
            @AuthenticationPrincipal AppUserDetails userDetails) {

        return ResponseEntity.ok(onboardingService.getProgress(userDetails.getId()));
    }

    @PostMapping("/steps/{stepNumber}/start")
    @Operation(summary = "Start a registration step (checks dependencies)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Step started"),
                    @ApiResponse(responseCode = "400", description = "Dependencies not met or step blocked"),
                    @ApiResponse(responseCode = "404", description = "Step not found")
            })
    public ResponseEntity<StepResponse> startStep(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable int stepNumber) {

        return ResponseEntity.ok(onboardingService.startStep(userDetails.getId(), stepNumber));
    }

    @PostMapping("/steps/{stepNumber}/complete")
    @Operation(summary = "Mark a registration step as completed",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Step completed"),
                    @ApiResponse(responseCode = "404", description = "Step not found")
            })
    public ResponseEntity<StepResponse> completeStep(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable int stepNumber) {

        return ResponseEntity.ok(onboardingService.completeStep(userDetails.getId(), stepNumber));
    }

    @PostMapping("/decisions/{decisionPointId}")
    @Operation(summary = "Record a user decision on a decision point",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Decision recorded"),
                    @ApiResponse(responseCode = "404", description = "Decision point not found")
            })
    public ResponseEntity<DecisionPointResponse> makeDecision(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable Long decisionPointId,
            @Valid @RequestBody DecisionRequest request) {

        return ResponseEntity.ok(onboardingService.makeDecision(
                userDetails.getId(), decisionPointId, request.choice()));
    }

    @PostMapping("/decisions/kleinunternehmer/evaluate")
    @Operation(summary = "Evaluate Kleinunternehmerregelung decision based on projected data",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Decision analysis produced")
            })
    public ResponseEntity<KurDecisionResponse> evaluateKleinunternehmer(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @Valid @RequestBody KurDecisionInput input) {

        return ResponseEntity.ok(decisionEngineService.evaluateKleinunternehmer(
                userDetails.getId(), input));
    }

    @GetMapping("/decisions/kleinunternehmer/evaluate")
    @Operation(summary = "Evaluate Kleinunternehmerregelung based on actual data for a given year",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Decision analysis from actual data")
            })
    public ResponseEntity<KurDecisionResponse> evaluateFromActualData(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @RequestParam(required = false) Integer year) {

        int evalYear = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(decisionEngineService.evaluateFromActualData(
                userDetails.getId(), evalYear));
    }
}
