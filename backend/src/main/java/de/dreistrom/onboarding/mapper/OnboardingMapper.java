package de.dreistrom.onboarding.mapper;

import de.dreistrom.onboarding.domain.DecisionPoint;
import de.dreistrom.onboarding.domain.RegistrationStep;
import de.dreistrom.onboarding.dto.DecisionPointResponse;
import de.dreistrom.onboarding.dto.StepResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OnboardingMapper {

    @Mapping(target = "decisionPoints", source = "decisionPoints")
    StepResponse toStepResponse(RegistrationStep step, List<DecisionPointResponse> decisionPoints);

    @Mapping(target = "stepId", source = "step.id")
    DecisionPointResponse toDecisionPointResponse(DecisionPoint decisionPoint);

    List<DecisionPointResponse> toDecisionPointResponseList(List<DecisionPoint> decisionPoints);
}
