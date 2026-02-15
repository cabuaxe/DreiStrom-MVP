package de.dreistrom.invoicing.service;

import de.dreistrom.invoicing.domain.InvoiceSequence;
import de.dreistrom.invoicing.domain.InvoiceStream;
import de.dreistrom.invoicing.repository.InvoiceSequenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generates sequential invoice numbers per stream type and fiscal year.
 * Uses pessimistic locking (SELECT ... FOR UPDATE) to prevent duplicates
 * under concurrent access.
 *
 * Format: FR-YYYY-NNN (Freiberuf) or GW-YYYY-NNN (Gewerbe)
 */
@Service
@RequiredArgsConstructor
public class InvoiceNumberGenerator {

    private final InvoiceSequenceRepository sequenceRepository;

    /**
     * Generate the next invoice number for the given stream and year.
     * Creates the sequence row if it doesn't exist yet.
     *
     * @param streamType FREIBERUF or GEWERBE
     * @param fiscalYear the fiscal year (e.g. 2026)
     * @return formatted invoice number, e.g. "FR-2026-001"
     */
    @Transactional
    public String nextInvoiceNumber(InvoiceStream streamType, int fiscalYear) {
        InvoiceSequence sequence = sequenceRepository.findForUpdate(streamType, fiscalYear)
                .orElseGet(() -> sequenceRepository.save(
                        new InvoiceSequence(streamType, fiscalYear)));

        int current = sequence.getAndIncrement();

        String prefix = switch (streamType) {
            case FREIBERUF -> "FR";
            case GEWERBE -> "GW";
        };

        return String.format("%s-%d-%03d", prefix, fiscalYear, current);
    }
}
