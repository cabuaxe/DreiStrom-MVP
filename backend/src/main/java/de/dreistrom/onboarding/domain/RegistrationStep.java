package de.dreistrom.onboarding.domain;

import de.dreistrom.common.domain.AppUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "registration_step")
@Getter
@NoArgsConstructor
public class RegistrationStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "step_number", nullable = false)
    private int stepNumber;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StepStatus status = StepStatus.NOT_STARTED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Responsible responsible = Responsible.USER;

    @Column(columnDefinition = "JSON")
    private String dependencies;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private Instant updatedAt;

    public RegistrationStep(AppUser user, int stepNumber, String title,
                            String description, Responsible responsible, String dependencies) {
        this.user = user;
        this.stepNumber = stepNumber;
        this.title = title;
        this.description = description;
        this.responsible = responsible;
        this.dependencies = dependencies;
        this.status = StepStatus.NOT_STARTED;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void start() {
        if (this.status == StepStatus.BLOCKED) {
            throw new IllegalStateException("Cannot start blocked step: " + stepNumber);
        }
        this.status = StepStatus.IN_PROGRESS;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        this.status = StepStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void block() {
        this.status = StepStatus.BLOCKED;
        this.updatedAt = Instant.now();
    }

    public void unblock() {
        if (this.status == StepStatus.BLOCKED) {
            this.status = StepStatus.NOT_STARTED;
            this.updatedAt = Instant.now();
        }
    }
}
