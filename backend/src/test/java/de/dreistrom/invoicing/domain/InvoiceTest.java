package de.dreistrom.invoicing.domain;

import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.domain.IncomeStream;
import de.dreistrom.common.repository.AppUserRepository;
import de.dreistrom.income.domain.Client;
import de.dreistrom.income.repository.ClientRepository;
import de.dreistrom.invoicing.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class InvoiceTest {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private AppUser user;
    private Client client;

    @BeforeEach
    void setUp() {
        invoiceRepository.deleteAll();
        clientRepository.deleteAll();
        appUserRepository.deleteAll();
        user = appUserRepository.save(new AppUser(
                "invoice@dreistrom.de",
                passwordEncoder.encode("test1234"),
                "Invoice Tester"));
        client = clientRepository.save(
                new Client(user, "Acme GmbH", IncomeStream.FREIBERUF));
    }

    @Test
    void persist_andRetrieve_invoice() {
        List<LineItem> items = List.of(
                new LineItem("Beratung Q1", new BigDecimal("10"), new BigDecimal("150.00"), new BigDecimal("19")),
                new LineItem("Reisekosten", new BigDecimal("1"), new BigDecimal("250.00"), new BigDecimal("19"))
        );

        Invoice invoice = new Invoice(user, InvoiceStream.FREIBERUF, "FR-2026-001", client,
                LocalDate.of(2026, 3, 15), items,
                new BigDecimal("1750.00"), new BigDecimal("332.50"), new BigDecimal("2082.50"),
                VatTreatment.REGULAR);
        Invoice saved = invoiceRepository.save(invoice);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStreamType()).isEqualTo(InvoiceStream.FREIBERUF);
        assertThat(saved.getNumber()).isEqualTo("FR-2026-001");
        assertThat(saved.getInvoiceDate()).isEqualTo(LocalDate.of(2026, 3, 15));
        assertThat(saved.getStatus()).isEqualTo(InvoiceStatus.DRAFT);
        assertThat(saved.getVatTreatment()).isEqualTo(VatTreatment.REGULAR);
        assertThat(saved.getCurrency()).isEqualTo("EUR");
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void moneyConverter_storesCents_andRestoresEuros() {
        List<LineItem> items = List.of(
                new LineItem("Service", new BigDecimal("1"), new BigDecimal("1234.56"), new BigDecimal("19"))
        );

        Invoice invoice = new Invoice(user, InvoiceStream.GEWERBE, "GW-2026-001", client,
                LocalDate.of(2026, 4, 1), items,
                new BigDecimal("1234.56"), new BigDecimal("234.57"), new BigDecimal("1469.13"),
                VatTreatment.REGULAR);
        Invoice saved = invoiceRepository.save(invoice);
        invoiceRepository.flush();

        Invoice fetched = invoiceRepository.findById(saved.getId()).orElseThrow();
        assertThat(fetched.getNetTotal()).isEqualByComparingTo("1234.56");
        assertThat(fetched.getVat()).isEqualByComparingTo("234.57");
        assertThat(fetched.getGrossTotal()).isEqualByComparingTo("1469.13");
    }

    @Test
    void lineItems_jsonSerialization_roundTrip() {
        List<LineItem> items = List.of(
                new LineItem("Programmierung", new BigDecimal("40"), new BigDecimal("95.00"), new BigDecimal("19")),
                new LineItem("Code Review", new BigDecimal("8"), new BigDecimal("120.00"), new BigDecimal("19"))
        );

        Invoice invoice = new Invoice(user, InvoiceStream.FREIBERUF, "FR-2026-002", client,
                LocalDate.of(2026, 3, 20), items,
                new BigDecimal("4760.00"), new BigDecimal("904.40"), new BigDecimal("5664.40"),
                VatTreatment.REGULAR);
        Invoice saved = invoiceRepository.save(invoice);

        List<LineItem> restored = saved.getLineItems();
        assertThat(restored).hasSize(2);
        assertThat(restored.get(0).description()).isEqualTo("Programmierung");
        assertThat(restored.get(0).quantity()).isEqualByComparingTo("40");
        assertThat(restored.get(0).unitPrice()).isEqualByComparingTo("95.00");
        assertThat(restored.get(1).description()).isEqualTo("Code Review");
    }

    @Test
    void updateStatus_changesStatus() {
        List<LineItem> items = List.of(
                new LineItem("Service", new BigDecimal("1"), new BigDecimal("100.00"), new BigDecimal("19"))
        );

        Invoice invoice = new Invoice(user, InvoiceStream.FREIBERUF, "FR-2026-003", client,
                LocalDate.of(2026, 5, 1), items,
                new BigDecimal("100.00"), new BigDecimal("19.00"), new BigDecimal("119.00"),
                VatTreatment.REGULAR);
        Invoice saved = invoiceRepository.save(invoice);

        assertThat(saved.getStatus()).isEqualTo(InvoiceStatus.DRAFT);
        saved.updateStatus(InvoiceStatus.SENT);
        assertThat(saved.getStatus()).isEqualTo(InvoiceStatus.SENT);
    }

    @Test
    void findByUserIdAndStreamType_filtersCorrectly() {
        List<LineItem> items = List.of(
                new LineItem("Item", new BigDecimal("1"), new BigDecimal("100.00"), new BigDecimal("19"))
        );

        invoiceRepository.save(new Invoice(user, InvoiceStream.FREIBERUF, "FR-2026-010", client,
                LocalDate.of(2026, 3, 1), items,
                new BigDecimal("100.00"), new BigDecimal("19.00"), new BigDecimal("119.00"),
                VatTreatment.REGULAR));
        invoiceRepository.save(new Invoice(user, InvoiceStream.GEWERBE, "GW-2026-010", client,
                LocalDate.of(2026, 3, 15), items,
                new BigDecimal("200.00"), new BigDecimal("38.00"), new BigDecimal("238.00"),
                VatTreatment.REGULAR));

        List<Invoice> freiberuf = invoiceRepository.findByUserIdAndStreamType(
                user.getId(), InvoiceStream.FREIBERUF);
        assertThat(freiberuf).hasSize(1);
        assertThat(freiberuf.getFirst().getNumber()).isEqualTo("FR-2026-010");
    }

    @Test
    void findByNumber_returnsUniqueInvoice() {
        List<LineItem> items = List.of(
                new LineItem("Item", new BigDecimal("1"), new BigDecimal("500.00"), new BigDecimal("19"))
        );

        invoiceRepository.save(new Invoice(user, InvoiceStream.FREIBERUF, "FR-2026-099", client,
                LocalDate.of(2026, 6, 1), items,
                new BigDecimal("500.00"), new BigDecimal("95.00"), new BigDecimal("595.00"),
                VatTreatment.REGULAR));

        assertThat(invoiceRepository.findByNumber("FR-2026-099")).isPresent();
        assertThat(invoiceRepository.findByNumber("FR-2026-999")).isEmpty();
    }
}
