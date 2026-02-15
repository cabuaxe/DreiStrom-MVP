package de.dreistrom.invoicing.domain;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for invoice_sequence table.
 */
public class InvoiceSequenceId implements Serializable {

    private InvoiceStream streamType;
    private int fiscalYear;

    public InvoiceSequenceId() {}

    public InvoiceSequenceId(InvoiceStream streamType, int fiscalYear) {
        this.streamType = streamType;
        this.fiscalYear = fiscalYear;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InvoiceSequenceId that = (InvoiceSequenceId) o;
        return fiscalYear == that.fiscalYear && streamType == that.streamType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(streamType, fiscalYear);
    }
}
