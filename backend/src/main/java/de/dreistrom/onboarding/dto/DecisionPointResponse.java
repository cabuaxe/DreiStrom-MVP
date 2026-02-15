package de.dreistrom.onboarding.dto;

import de.dreistrom.onboarding.domain.DecisionChoice;

import java.time.Instant;

public record DecisionPointResponse(
        Long id,
        Long stepId,
        String question,
        String optionA,
        String optionB,
        DecisionChoice recommendation,
        String recommendationReason,
        DecisionChoice userChoice,
        Instant decidedAt
) {}
