package de.dreistrom.income.domain;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.domain.IncomeStream;
import de.dreistrom.common.domain.MoneyConverter;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "income_entry")
@Getter
@NoArgsConstructor
public class IncomeEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(name = "stream_type", nullable = false)
    private IncomeStream streamType;

    @Convert(converter = MoneyConverter.class)
    @Column(name = "amount_cents", nullable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency = "EUR";

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column
    private String source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @Column(name = "invoice_id")
    private Long invoiceId;

    @Column(length = 500)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private Instant updatedAt;

    public IncomeEntry(AppUser user, IncomeStream streamType, BigDecimal amount,
                       LocalDate entryDate) {
        this.user = user;
        this.streamType = streamType;
        this.amount = amount;
        this.entryDate = entryDate;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public IncomeEntry(AppUser user, IncomeStream streamType, BigDecimal amount,
                       LocalDate entryDate, String source, Client client, String description) {
        this(user, streamType, amount, entryDate);
        this.source = source;
        this.client = client;
        this.description = description;
    }

    /**
     * Soft-update: apply mutable fields. Stream type is immutable after creation.
     */
    public void update(BigDecimal amount, LocalDate entryDate, String source,
                       Client client, String description) {
        this.amount = amount;
        this.entryDate = entryDate;
        this.source = source;
        this.client = client;
        this.description = description;
        this.updatedAt = Instant.now();
    }
}
