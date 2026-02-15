package de.dreistrom.onboarding.dto;

import java.util.List;

public record OnboardingProgressResponse(
        long totalSteps,
        long completedSteps,
        long inProgressSteps,
        long blockedSteps,
        int progressPercent,
        List<StepResponse> steps
) {}
