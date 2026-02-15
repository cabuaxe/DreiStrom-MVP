package de.dreistrom.onboarding.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "decision_point")
@Getter
@NoArgsConstructor
public class DecisionPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_id", nullable = false)
    private RegistrationStep step;

    @Column(nullable = false, length = 500)
    private String question;

    @Column(name = "option_a", nullable = false, length = 500)
    private String optionA;

    @Column(name = "option_b", nullable = false, length = 500)
    private String optionB;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DecisionChoice recommendation;

    @Column(name = "recommendation_reason", columnDefinition = "TEXT")
    private String recommendationReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_choice")
    private DecisionChoice userChoice;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private Instant createdAt;

    public DecisionPoint(RegistrationStep step, String question,
                         String optionA, String optionB,
                         DecisionChoice recommendation, String recommendationReason) {
        this.step = step;
        this.question = question;
        this.optionA = optionA;
        this.optionB = optionB;
        this.recommendation = recommendation;
        this.recommendationReason = recommendationReason;
        this.createdAt = Instant.now();
    }

    public void decide(DecisionChoice choice) {
        this.userChoice = choice;
        this.decidedAt = Instant.now();
    }

    public boolean isDecided() {
        return userChoice != null;
    }
}
