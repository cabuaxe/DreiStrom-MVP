package de.dreistrom.onboarding.repository;

import de.dreistrom.onboarding.domain.DecisionPoint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DecisionPointRepository extends JpaRepository<DecisionPoint, Long> {

    List<DecisionPoint> findByStepId(Long stepId);

    List<DecisionPoint> findByStepIdIn(List<Long> stepIds);
}
