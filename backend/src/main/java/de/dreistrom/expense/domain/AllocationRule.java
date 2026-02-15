package de.dreistrom.expense.domain;

import de.dreistrom.common.domain.AppUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "allocation_rule")
@Getter
@NoArgsConstructor
public class AllocationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false)
    private String name;

    @Column(name = "freiberuf_pct", nullable = false)
    private short freiberufPct;

    @Column(name = "gewerbe_pct", nullable = false)
    private short gewerbePct;

    @Column(name = "personal_pct", nullable = false)
    private short personalPct;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private Instant updatedAt;

    public AllocationRule(AppUser user, String name, short freiberufPct,
                          short gewerbePct, short personalPct) {
        this.user = user;
        this.name = name;
        this.freiberufPct = freiberufPct;
        this.gewerbePct = gewerbePct;
        this.personalPct = personalPct;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void update(String name, short freiberufPct, short gewerbePct, short personalPct) {
        this.name = name;
        this.freiberufPct = freiberufPct;
        this.gewerbePct = gewerbePct;
        this.personalPct = personalPct;
        this.updatedAt = Instant.now();
    }

    @PrePersist
    @PreUpdate
    void validateAllocationSum() {
        if (freiberufPct + gewerbePct + personalPct != 100) {
            throw new IllegalStateException(
                    "Allocation percentages must sum to 100, got: " +
                    (freiberufPct + gewerbePct + personalPct));
        }
    }
}
