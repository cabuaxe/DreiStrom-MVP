package de.dreistrom.onboarding.dto;

import de.dreistrom.onboarding.domain.Responsible;
import de.dreistrom.onboarding.domain.StepStatus;

import java.time.Instant;
import java.util.List;

public record StepResponse(
        Long id,
        int stepNumber,
        String title,
        String description,
        StepStatus status,
        Responsible responsible,
        String dependencies,
        Instant completedAt,
        Instant createdAt,
        Instant updatedAt,
        List<DecisionPointResponse> decisionPoints
) {}
