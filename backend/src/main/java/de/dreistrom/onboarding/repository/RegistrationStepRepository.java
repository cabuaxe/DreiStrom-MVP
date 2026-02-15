package de.dreistrom.onboarding.repository;

import de.dreistrom.onboarding.domain.RegistrationStep;
import de.dreistrom.onboarding.domain.StepStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegistrationStepRepository extends JpaRepository<RegistrationStep, Long> {

    List<RegistrationStep> findByUserIdOrderByStepNumber(Long userId);

    Optional<RegistrationStep> findByUserIdAndStepNumber(Long userId, int stepNumber);

    List<RegistrationStep> findByUserIdAndStatus(Long userId, StepStatus status);

    long countByUserIdAndStatus(Long userId, StepStatus status);

    long countByUserId(Long userId);
}
