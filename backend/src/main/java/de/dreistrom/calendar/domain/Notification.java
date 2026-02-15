package de.dreistrom.calendar.domain;

import de.dreistrom.common.domain.AppUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "notification")
@Getter
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compliance_event_id")
    private ComplianceEvent complianceEvent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "days_before", nullable = false)
    private int daysBefore;

    @Column(nullable = false)
    private boolean delivered = false;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private Instant createdAt;

    public Notification(AppUser user, ComplianceEvent event, NotificationChannel channel,
                        String title, String message, int daysBefore) {
        this.user = user;
        this.complianceEvent = event;
        this.channel = channel;
        this.title = title;
        this.message = message;
        this.daysBefore = daysBefore;
        this.createdAt = Instant.now();
    }

    public void markDelivered() {
        this.delivered = true;
    }

    public void markRead() {
        this.readAt = Instant.now();
    }
}
