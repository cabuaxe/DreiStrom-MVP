package de.dreistrom.invoicing.service;

import de.dreistrom.invoicing.domain.InvoiceSequence;
import de.dreistrom.invoicing.domain.InvoiceStream;
import de.dreistrom.invoicing.repository.InvoiceSequenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class InvoiceNumberGeneratorTest {

    @Autowired
    private InvoiceNumberGenerator generator;

    @Autowired
    private InvoiceSequenceRepository sequenceRepository;

    @BeforeEach
    void setUp() {
        sequenceRepository.deleteAll();
    }

    @Test
    void freiberuf_prefixIsFR() {
        String number = generator.nextInvoiceNumber(InvoiceStream.FREIBERUF, 2026);
        assertThat(number).isEqualTo("FR-2026-001");
    }

    @Test
    void gewerbe_prefixIsGW() {
        String number = generator.nextInvoiceNumber(InvoiceStream.GEWERBE, 2026);
        assertThat(number).isEqualTo("GW-2026-001");
    }

    @Test
    void sequential_numbersIncrement() {
        assertThat(generator.nextInvoiceNumber(InvoiceStream.FREIBERUF, 2026)).isEqualTo("FR-2026-001");
        assertThat(generator.nextInvoiceNumber(InvoiceStream.FREIBERUF, 2026)).isEqualTo("FR-2026-002");
        assertThat(generator.nextInvoiceNumber(InvoiceStream.FREIBERUF, 2026)).isEqualTo("FR-2026-003");
    }

    @Test
    void differentStreams_haveSeparateSequences() {
        assertThat(generator.nextInvoiceNumber(InvoiceStream.FREIBERUF, 2026)).isEqualTo("FR-2026-001");
        assertThat(generator.nextInvoiceNumber(InvoiceStream.GEWERBE, 2026)).isEqualTo("GW-2026-001");
        assertThat(generator.nextInvoiceNumber(InvoiceStream.FREIBERUF, 2026)).isEqualTo("FR-2026-002");
        assertThat(generator.nextInvoiceNumber(InvoiceStream.GEWERBE, 2026)).isEqualTo("GW-2026-002");
    }

    @Test
    void differentYears_haveSeparateSequences() {
        assertThat(generator.nextInvoiceNumber(InvoiceStream.FREIBERUF, 2026)).isEqualTo("FR-2026-001");
        assertThat(generator.nextInvoiceNumber(InvoiceStream.FREIBERUF, 2027)).isEqualTo("FR-2027-001");
        assertThat(generator.nextInvoiceNumber(InvoiceStream.FREIBERUF, 2026)).isEqualTo("FR-2026-002");
    }

    @Test
    void existingSequence_continuesFromStoredValue() {
        // Pre-seed sequence at value 42
        InvoiceSequence existing = new InvoiceSequence(InvoiceStream.GEWERBE, 2026);
        for (int i = 0; i < 41; i++) existing.getAndIncrement(); // advance to 42
        sequenceRepository.save(existing);
        sequenceRepository.flush();

        String number = generator.nextInvoiceNumber(InvoiceStream.GEWERBE, 2026);
        assertThat(number).isEqualTo("GW-2026-042");
    }

    @Test
    void numberFormat_padsWith3Digits() {
        String number = generator.nextInvoiceNumber(InvoiceStream.FREIBERUF, 2026);
        assertThat(number).matches("FR-2026-\\d{3}");
    }
}
