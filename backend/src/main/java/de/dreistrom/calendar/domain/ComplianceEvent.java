package de.dreistrom.calendar.domain;

import de.dreistrom.common.domain.AppUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "compliance_event")
@Getter
@NoArgsConstructor
public class ComplianceEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private ComplianceEventType eventType;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ComplianceEventStatus status = ComplianceEventStatus.UPCOMING;

    @Column(name = "tax_period_id")
    private Long taxPeriodId;

    @Column(name = "reminder_config", columnDefinition = "JSON")
    private String reminderConfig;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private Instant updatedAt;

    public ComplianceEvent(AppUser user, ComplianceEventType eventType,
                           String title, LocalDate dueDate) {
        this.user = user;
        this.eventType = eventType;
        this.title = title;
        this.dueDate = dueDate;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public ComplianceEvent(AppUser user, ComplianceEventType eventType,
                           String title, String description, LocalDate dueDate,
                           Long taxPeriodId, String reminderConfig) {
        this(user, eventType, title, dueDate);
        this.description = description;
        this.taxPeriodId = taxPeriodId;
        this.reminderConfig = reminderConfig;
    }

    public void markDue() {
        this.status = ComplianceEventStatus.DUE;
        this.updatedAt = Instant.now();
    }

    public void markOverdue() {
        this.status = ComplianceEventStatus.OVERDUE;
        this.updatedAt = Instant.now();
    }

    public void markCompleted() {
        this.status = ComplianceEventStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void cancel() {
        this.status = ComplianceEventStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    public void updateReminderConfig(String config) {
        this.reminderConfig = config;
        this.updatedAt = Instant.now();
    }

    public void reschedule(LocalDate newDueDate) {
        this.dueDate = newDueDate;
        this.status = ComplianceEventStatus.UPCOMING;
        this.updatedAt = Instant.now();
    }
}
