package de.dreistrom.invoicing.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Tracks the next invoice number per stream type and fiscal year.
 * Composite PK: (stream_type, fiscal_year).
 */
@Entity
@Table(name = "invoice_sequence")
@IdClass(InvoiceSequenceId.class)
@Getter
@NoArgsConstructor
public class InvoiceSequence {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "stream_type", nullable = false)
    private InvoiceStream streamType;

    @Id
    @Column(name = "fiscal_year", nullable = false)
    private int fiscalYear;

    @Column(name = "next_value", nullable = false)
    private int nextValue = 1;

    public InvoiceSequence(InvoiceStream streamType, int fiscalYear) {
        this.streamType = streamType;
        this.fiscalYear = fiscalYear;
        this.nextValue = 1;
    }

    /**
     * Returns the current value and increments the counter.
     */
    public int getAndIncrement() {
        int current = this.nextValue;
        this.nextValue = current + 1;
        return current;
    }
}
