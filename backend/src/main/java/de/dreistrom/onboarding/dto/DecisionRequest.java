package de.dreistrom.onboarding.dto;

import de.dreistrom.onboarding.domain.DecisionChoice;
import jakarta.validation.constraints.NotNull;

public record DecisionRequest(
        @NotNull DecisionChoice choice
) {}
