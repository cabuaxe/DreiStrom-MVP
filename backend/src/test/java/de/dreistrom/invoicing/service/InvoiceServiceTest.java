package de.dreistrom.invoicing.service;

import de.dreistrom.audit.domain.EventLog;
import de.dreistrom.audit.repository.EventLogRepository;
import de.dreistrom.common.controller.EntityNotFoundException;
import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.domain.IncomeStream;
import de.dreistrom.common.repository.AppUserRepository;
import de.dreistrom.income.domain.Client;
import de.dreistrom.income.domain.ClientType;
import de.dreistrom.income.domain.IncomeEntry;
import de.dreistrom.income.repository.ClientRepository;
import de.dreistrom.income.repository.IncomeEntryRepository;
import de.dreistrom.invoicing.domain.Invoice;
import de.dreistrom.invoicing.domain.InvoiceStatus;
import de.dreistrom.invoicing.domain.InvoiceStream;
import de.dreistrom.invoicing.domain.LineItem;
import de.dreistrom.invoicing.domain.VatTreatment;
import de.dreistrom.invoicing.repository.InvoiceRepository;
import de.dreistrom.invoicing.repository.InvoiceSequenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class InvoiceServiceTest {

    @Autowired private InvoiceService invoiceService;
    @Autowired private InvoiceRepository invoiceRepository;
    @Autowired private InvoiceSequenceRepository sequenceRepository;
    @Autowired private IncomeEntryRepository incomeEntryRepository;
    @Autowired private ClientRepository clientRepository;
    @Autowired private AppUserRepository appUserRepository;
    @Autowired private EventLogRepository eventLogRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private AppUser user;
    private Client frClient;
    private Client gwClient;

    private static final List<LineItem> VALID_ITEMS = List.of(
            new LineItem("Beratung Q1", new BigDecimal("10"), new BigDecimal("150.00"), new BigDecimal("19")));

    @BeforeEach
    void setUp() {
        invoiceRepository.deleteAll();
        incomeEntryRepository.deleteAll();
        sequenceRepository.deleteAll();
        clientRepository.deleteAll();
        appUserRepository.deleteAll();
        eventLogRepository.deleteAll();

        user = appUserRepository.save(new AppUser(
                "invoice@dreistrom.de",
                passwordEncoder.encode("test1234"),
                "Invoice Tester"));
        frClient = clientRepository.save(new Client(user, "FR Kunde GmbH", IncomeStream.FREIBERUF));
        gwClient = clientRepository.save(new Client(user, "GW Kunde GmbH", IncomeStream.GEWERBE));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("invoice@dreistrom.de", null, List.of()));
    }

    @Nested
    class Creation {

        @Test
        void create_freiberuf_persistsInvoiceAndAuditEvent() {
            Invoice invoice = invoiceService.create(user, InvoiceStream.FREIBERUF, frClient.getId(),
                    LocalDate.of(2026, 3, 15), null, VALID_ITEMS,
                    new BigDecimal("1500.00"), new BigDecimal("285.00"), new BigDecimal("1785.00"),
                    VatTreatment.REGULAR, null);

            assertThat(invoice.getId()).isNotNull();
            assertThat(invoice.getNumber()).isEqualTo("FR-2026-001");
            assertThat(invoice.getStreamType()).isEqualTo(InvoiceStream.FREIBERUF);
            assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.DRAFT);
            assertThat(invoice.getNetTotal()).isEqualByComparingTo("1500.00");
            assertThat(invoice.getVat()).isEqualByComparingTo("285.00");
            assertThat(invoice.getGrossTotal()).isEqualByComparingTo("1785.00");

            List<EventLog> events = eventLogRepository
                    .findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc("Invoice", invoice.getId());
            assertThat(events).hasSize(1);
            assertThat(events.getFirst().getEventType()).isEqualTo("INVOICE_CREATED");
            assertThat(events.getFirst().getPayload()).contains("FR-2026-001");
        }

        @Test
        void create_gewerbe_generatesGWPrefix() {
            Invoice invoice = invoiceService.create(user, InvoiceStream.GEWERBE, gwClient.getId(),
                    LocalDate.of(2026, 4, 1), null, VALID_ITEMS,
                    new BigDecimal("1000.00"), new BigDecimal("190.00"), new BigDecimal("1190.00"),
                    VatTreatment.REGULAR, null);

            assertThat(invoice.getNumber()).isEqualTo("GW-2026-001");
        }

        @Test
        void create_autoCreatesLinkedIncomeEntry() {
            Invoice invoice = invoiceService.create(user, InvoiceStream.FREIBERUF, frClient.getId(),
                    LocalDate.of(2026, 3, 15), null, VALID_ITEMS,
                    new BigDecimal("1500.00"), new BigDecimal("285.00"), new BigDecimal("1785.00"),
                    VatTreatment.REGULAR, null);

            List<IncomeEntry> entries = incomeEntryRepository.findByUserId(user.getId());
            assertThat(entries).hasSize(1);

            IncomeEntry entry = entries.getFirst();
            assertThat(entry.getStreamType()).isEqualTo(IncomeStream.FREIBERUF);
            assertThat(entry.getAmount()).isEqualByComparingTo("1785.00");
            assertThat(entry.getInvoiceId()).isEqualTo(invoice.getId());
            assertThat(entry.getSource()).contains("FR-2026-001");
            assertThat(entry.getClient().getId()).isEqualTo(frClient.getId());
        }

        @Test
        void create_gewerbe_incomeEntryHasGewerbeStream() {
            invoiceService.create(user, InvoiceStream.GEWERBE, gwClient.getId(),
                    LocalDate.of(2026, 5, 1), null, VALID_ITEMS,
                    new BigDecimal("800.00"), new BigDecimal("152.00"), new BigDecimal("952.00"),
                    VatTreatment.REGULAR, null);

            List<IncomeEntry> entries = incomeEntryRepository.findByUserId(user.getId());
            assertThat(entries.getFirst().getStreamType()).isEqualTo(IncomeStream.GEWERBE);
        }

        @Test
        void create_sequentialNumbers_incrementCorrectly() {
            invoiceService.create(user, InvoiceStream.FREIBERUF, frClient.getId(),
                    LocalDate.of(2026, 3, 1), null, VALID_ITEMS,
                    new BigDecimal("100.00"), new BigDecimal("19.00"), new BigDecimal("119.00"),
                    VatTreatment.REGULAR, null);
            Invoice second = invoiceService.create(user, InvoiceStream.FREIBERUF, frClient.getId(),
                    LocalDate.of(2026, 3, 15), null, VALID_ITEMS,
                    new BigDecimal("200.00"), new BigDecimal("38.00"), new BigDecimal("238.00"),
                    VatTreatment.REGULAR, null);

            assertThat(second.getNumber()).isEqualTo("FR-2026-002");
        }

        @Test
        void create_withDueDate_setsDueDate() {
            Invoice invoice = invoiceService.create(user, InvoiceStream.FREIBERUF, frClient.getId(),
                    LocalDate.of(2026, 3, 15), LocalDate.of(2026, 4, 14), VALID_ITEMS,
                    new BigDecimal("1500.00"), new BigDecimal("285.00"), new BigDecimal("1785.00"),
                    VatTreatment.REGULAR, null);

            assertThat(invoice.getDueDate()).isEqualTo(LocalDate.of(2026, 4, 14));
        }
    }

    @Nested
    class UStG14Validation {

        @Test
        void create_withoutClient_rejects() {
            assertThatThrownBy(() -> invoiceService.create(user, InvoiceStream.FREIBERUF, null,
                    LocalDate.of(2026, 3, 15), null, VALID_ITEMS,
                    new BigDecimal("100.00"), new BigDecimal("19.00"), new BigDecimal("119.00"),
                    VatTreatment.REGULAR, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Leistungsempfänger")
                    .hasMessageContaining("§14");
        }

        @Test
        void create_withoutInvoiceDate_rejects() {
            assertThatThrownBy(() -> invoiceService.create(user, InvoiceStream.FREIBERUF, frClient.getId(),
                    null, null, VALID_ITEMS,
                    new BigDecimal("100.00"), new BigDecimal("19.00"), new BigDecimal("119.00"),
                    VatTreatment.REGULAR, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Rechnungsdatum");
        }

        @Test
        void create_withEmptyLineItems_rejects() {
            assertThatThrownBy(() -> invoiceService.create(user, InvoiceStream.FREIBERUF, frClient.getId(),
                    LocalDate.of(2026, 3, 15), null, List.of(),
                    new BigDecimal("100.00"), new BigDecimal("19.00"), new BigDecimal("119.00"),
                    VatTreatment.REGULAR, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Leistungsbeschreibung");
        }

        @Test
        void create_lineItemWithBlankDescription_rejects() {
            List<LineItem> items = List.of(
                    new LineItem("", new BigDecimal("1"), new BigDecimal("100.00"), new BigDecimal("19")));

            assertThatThrownBy(() -> invoiceService.create(user, InvoiceStream.FREIBERUF, frClient.getId(),
                    LocalDate.of(2026, 3, 15), null, items,
                    new BigDecimal("100.00"), new BigDecimal("19.00"), new BigDecimal("119.00"),
                    VatTreatment.REGULAR, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("description is required");
        }

        @Test
        void create_lineItemWithZeroQuantity_rejects() {
            List<LineItem> items = List.of(
                    new LineItem("Service", BigDecimal.ZERO, new BigDecimal("100.00"), new BigDecimal("19")));

            assertThatThrownBy(() -> invoiceService.create(user, InvoiceStream.FREIBERUF, frClient.getId(),
                    LocalDate.of(2026, 3, 15), null, items,
                    new BigDecimal("100.00"), new BigDecimal("19.00"), new BigDecimal("119.00"),
                    VatTreatment.REGULAR, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("quantity must be positive");
        }

        @Test
        void create_negativeNetTotal_rejects() {
            assertThatThrownBy(() -> invoiceService.create(user, InvoiceStream.FREIBERUF, frClient.getId(),
                    LocalDate.of(2026, 3, 15), null, VALID_ITEMS,
                    new BigDecimal("-100.00"), new BigDecimal("0"), new BigDecimal("-100.00"),
                    VatTreatment.REGULAR, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Entgelt");
        }

        @Test
        void create_wrongClientStream_rejects() {
            assertThatThrownBy(() -> invoiceService.create(user, InvoiceStream.FREIBERUF, gwClient.getId(),
                    LocalDate.of(2026, 3, 15), null, VALID_ITEMS,
                    new BigDecimal("100.00"), new BigDecimal("19.00"), new BigDecimal("119.00"),
                    VatTreatment.REGULAR, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("does not match invoice stream type");
        }
    }

    @Nested
    class Kleinunternehmer {

        @Test
        void create_kleinunternehmer_withParagraph19Notice_succeeds() {
            List<LineItem> items = List.of(
                    new LineItem("Beratung", new BigDecimal("10"), new BigDecimal("100.00"), BigDecimal.ZERO));

            Invoice invoice = invoiceService.create(user, InvoiceStream.FREIBERUF, frClient.getId(),
                    LocalDate.of(2026, 3, 15), null, items,
                    new BigDecimal("1000.00"), BigDecimal.ZERO, new BigDecimal("1000.00"),
                    VatTreatment.SMALL_BUSINESS,
                    "Gemäß §19 UStG wird keine Umsatzsteuer berechnet.");

            assertThat(invoice.getVatTreatment()).isEqualTo(VatTreatment.SMALL_BUSINESS);
            assertThat(invoice.getVat()).isEqualByComparingTo("0");
        }

        @Test
        void create_kleinunternehmer_withNonZeroVat_rejects() {
            List<LineItem> items = List.of(
                    new LineItem("Beratung", new BigDecimal("10"), new BigDecimal("100.00"), BigDecimal.ZERO));

            assertThatThrownBy(() -> invoiceService.create(user, InvoiceStream.FREIBERUF, frClient.getId(),
                    LocalDate.of(2026, 3, 15), null, items,
                    new BigDecimal("1000.00"), new BigDecimal("190.00"), new BigDecimal("1190.00"),
                    VatTreatment.SMALL_BUSINESS,
                    "Gemäß §19 UStG wird keine Umsatzsteuer berechnet."))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Kleinunternehmer")
                    .hasMessageContaining("VAT must be 0")
                    .hasMessageContaining("§19");
        }

        @Test
        void create_kleinunternehmer_withoutParagraph19Notice_rejects() {
            List<LineItem> items = List.of(
                    new LineItem("Beratung", new BigDecimal("10"), new BigDecimal("100.00"), BigDecimal.ZERO));

            assertThatThrownBy(() -> invoiceService.create(user, InvoiceStream.FREIBERUF, frClient.getId(),
                    LocalDate.of(2026, 3, 15), null, items,
                    new BigDecimal("1000.00"), BigDecimal.ZERO, new BigDecimal("1000.00"),
                    VatTreatment.SMALL_BUSINESS, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("§19 UStG notice");
        }

        @Test
        void create_kleinunternehmer_withNonZeroVatRateOnItem_rejects() {
            List<LineItem> items = List.of(
                    new LineItem("Beratung", new BigDecimal("10"), new BigDecimal("100.00"), new BigDecimal("19")));

            assertThatThrownBy(() -> invoiceService.create(user, InvoiceStream.FREIBERUF, frClient.getId(),
                    LocalDate.of(2026, 3, 15), null, items,
                    new BigDecimal("1000.00"), BigDecimal.ZERO, new BigDecimal("1000.00"),
                    VatTreatment.SMALL_BUSINESS,
                    "Gemäß §19 UStG wird keine Umsatzsteuer berechnet."))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Kleinunternehmer")
                    .hasMessageContaining("VAT rate must be 0");
        }
    }

    @Nested
    class ReverseChargeAndIntraEU {

        @Test
        void create_reverseCharge_withZeroVat_succeeds() {
            List<LineItem> items = List.of(
                    new LineItem("Consulting", new BigDecimal("40"), new BigDecimal("120.00"), BigDecimal.ZERO));

            Invoice invoice = invoiceService.create(user, InvoiceStream.FREIBERUF, frClient.getId(),
                    LocalDate.of(2026, 3, 15), null, items,
                    new BigDecimal("4800.00"), BigDecimal.ZERO, new BigDecimal("4800.00"),
                    VatTreatment.REVERSE_CHARGE, null);

            assertThat(invoice.getVatTreatment()).isEqualTo(VatTreatment.REVERSE_CHARGE);
        }

        @Test
        void create_reverseCharge_withNonZeroVat_rejects() {
            assertThatThrownBy(() -> invoiceService.create(user, InvoiceStream.FREIBERUF, frClient.getId(),
                    LocalDate.of(2026, 3, 15), null, VALID_ITEMS,
                    new BigDecimal("1500.00"), new BigDecimal("285.00"), new BigDecimal("1785.00"),
                    VatTreatment.REVERSE_CHARGE, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Reverse charge")
                    .hasMessageContaining("VAT must be 0");
        }

        @Test
        void create_intraEU_withUstIdNr_succeeds() {
            Client euClient = clientRepository.save(new Client(user, "EU Corp",
                    IncomeStream.FREIBERUF, de.dreistrom.income.domain.ClientType.B2B, "AT", "ATU12345678"));

            List<LineItem> items = List.of(
                    new LineItem("Service", new BigDecimal("1"), new BigDecimal("5000.00"), BigDecimal.ZERO));

            Invoice invoice = invoiceService.create(user, InvoiceStream.FREIBERUF, euClient.getId(),
                    LocalDate.of(2026, 3, 15), null, items,
                    new BigDecimal("5000.00"), BigDecimal.ZERO, new BigDecimal("5000.00"),
                    VatTreatment.INTRA_EU, null);

            assertThat(invoice.getVatTreatment()).isEqualTo(VatTreatment.INTRA_EU);
        }

        @Test
        void create_intraEU_withoutUstIdNr_rejects() {
            assertThatThrownBy(() -> invoiceService.create(user, InvoiceStream.FREIBERUF, frClient.getId(),
                    LocalDate.of(2026, 3, 15), null,
                    List.of(new LineItem("Service", new BigDecimal("1"), new BigDecimal("5000.00"), BigDecimal.ZERO)),
                    new BigDecimal("5000.00"), BigDecimal.ZERO, new BigDecimal("5000.00"),
                    VatTreatment.INTRA_EU, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("USt-IdNr")
                    .hasMessageContaining("§14a");
        }
    }

    @Nested
    class UpdateAndDelete {

        @Test
        void update_draftInvoice_succeeds() {
            Invoice invoice = createValidInvoice();

            List<LineItem> newItems = List.of(
                    new LineItem("Beratung Q2", new BigDecimal("20"), new BigDecimal("200.00"), new BigDecimal("19")));

            Invoice updated = invoiceService.update(invoice.getId(), user.getId(), frClient.getId(),
                    LocalDate.of(2026, 4, 1), LocalDate.of(2026, 5, 1), newItems,
                    new BigDecimal("4000.00"), new BigDecimal("760.00"), new BigDecimal("4760.00"),
                    VatTreatment.REGULAR, "Updated invoice");

            assertThat(updated.getNetTotal()).isEqualByComparingTo("4000.00");
            assertThat(updated.getNotes()).isEqualTo("Updated invoice");
        }

        @Test
        void update_sentInvoice_rejects() {
            Invoice invoice = createValidInvoice();
            invoiceService.updateStatus(invoice.getId(), user.getId(), InvoiceStatus.SENT);

            assertThatThrownBy(() -> invoiceService.update(invoice.getId(), user.getId(), frClient.getId(),
                    LocalDate.of(2026, 4, 1), null, VALID_ITEMS,
                    new BigDecimal("1500.00"), new BigDecimal("285.00"), new BigDecimal("1785.00"),
                    VatTreatment.REGULAR, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Only DRAFT invoices can be updated");
        }

        @Test
        void delete_draftInvoice_succeeds() {
            Invoice invoice = createValidInvoice();

            invoiceService.delete(invoice.getId(), user.getId());

            assertThat(invoiceRepository.findById(invoice.getId())).isEmpty();
        }

        @Test
        void delete_sentInvoice_rejects() {
            Invoice invoice = createValidInvoice();
            invoiceService.updateStatus(invoice.getId(), user.getId(), InvoiceStatus.SENT);

            assertThatThrownBy(() -> invoiceService.delete(invoice.getId(), user.getId()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Only DRAFT invoices can be deleted");
        }

        @Test
        void getById_wrongUser_throwsNotFound() {
            Invoice invoice = createValidInvoice();
            assertThatThrownBy(() -> invoiceService.getById(invoice.getId(), 999L))
                    .isInstanceOf(EntityNotFoundException.class);
        }

        @Test
        void getById_nonExistent_throwsNotFound() {
            assertThatThrownBy(() -> invoiceService.getById(999L, user.getId()))
                    .isInstanceOf(EntityNotFoundException.class);
        }
    }

    @Nested
    class StatusTransitions {

        @Test
        void draft_to_sent_succeeds() {
            Invoice invoice = createValidInvoice();
            Invoice updated = invoiceService.updateStatus(invoice.getId(), user.getId(), InvoiceStatus.SENT);
            assertThat(updated.getStatus()).isEqualTo(InvoiceStatus.SENT);
        }

        @Test
        void sent_to_paid_succeeds() {
            Invoice invoice = createValidInvoice();
            invoiceService.updateStatus(invoice.getId(), user.getId(), InvoiceStatus.SENT);
            Invoice updated = invoiceService.updateStatus(invoice.getId(), user.getId(), InvoiceStatus.PAID);
            assertThat(updated.getStatus()).isEqualTo(InvoiceStatus.PAID);
        }

        @Test
        void sent_to_overdue_succeeds() {
            Invoice invoice = createValidInvoice();
            invoiceService.updateStatus(invoice.getId(), user.getId(), InvoiceStatus.SENT);
            Invoice updated = invoiceService.updateStatus(invoice.getId(), user.getId(), InvoiceStatus.OVERDUE);
            assertThat(updated.getStatus()).isEqualTo(InvoiceStatus.OVERDUE);
        }

        @Test
        void overdue_to_paid_succeeds() {
            Invoice invoice = createValidInvoice();
            invoiceService.updateStatus(invoice.getId(), user.getId(), InvoiceStatus.SENT);
            invoiceService.updateStatus(invoice.getId(), user.getId(), InvoiceStatus.OVERDUE);
            Invoice updated = invoiceService.updateStatus(invoice.getId(), user.getId(), InvoiceStatus.PAID);
            assertThat(updated.getStatus()).isEqualTo(InvoiceStatus.PAID);
        }

        @Test
        void draft_to_cancelled_succeeds() {
            Invoice invoice = createValidInvoice();
            Invoice updated = invoiceService.updateStatus(invoice.getId(), user.getId(), InvoiceStatus.CANCELLED);
            assertThat(updated.getStatus()).isEqualTo(InvoiceStatus.CANCELLED);
        }

        @Test
        void draft_to_paid_rejects() {
            Invoice invoice = createValidInvoice();
            assertThatThrownBy(() -> invoiceService.updateStatus(invoice.getId(), user.getId(), InvoiceStatus.PAID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid status transition");
        }

        @Test
        void paid_to_sent_rejects() {
            Invoice invoice = createValidInvoice();
            invoiceService.updateStatus(invoice.getId(), user.getId(), InvoiceStatus.SENT);
            invoiceService.updateStatus(invoice.getId(), user.getId(), InvoiceStatus.PAID);

            assertThatThrownBy(() -> invoiceService.updateStatus(invoice.getId(), user.getId(), InvoiceStatus.SENT))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid status transition");
        }
    }

    @Nested
    class QueryMethods {

        @Test
        void listAll_returnsAllUserInvoices() {
            createValidInvoice();
            invoiceService.create(user, InvoiceStream.GEWERBE, gwClient.getId(),
                    LocalDate.of(2026, 4, 1), null, VALID_ITEMS,
                    new BigDecimal("500.00"), new BigDecimal("95.00"), new BigDecimal("595.00"),
                    VatTreatment.REGULAR, null);

            assertThat(invoiceService.listAll(user.getId())).hasSize(2);
        }

        @Test
        void listByStream_filtersCorrectly() {
            createValidInvoice(); // FREIBERUF
            invoiceService.create(user, InvoiceStream.GEWERBE, gwClient.getId(),
                    LocalDate.of(2026, 4, 1), null, VALID_ITEMS,
                    new BigDecimal("500.00"), new BigDecimal("95.00"), new BigDecimal("595.00"),
                    VatTreatment.REGULAR, null);

            List<Invoice> freiberuf = invoiceService.listByStream(user.getId(), InvoiceStream.FREIBERUF);
            assertThat(freiberuf).hasSize(1);
            assertThat(freiberuf.getFirst().getStreamType()).isEqualTo(InvoiceStream.FREIBERUF);
        }

        @Test
        void listByStatus_filtersCorrectly() {
            Invoice draft = createValidInvoice();
            Invoice sent = createValidInvoice();
            invoiceService.updateStatus(sent.getId(), user.getId(), InvoiceStatus.SENT);

            assertThat(invoiceService.listByStatus(user.getId(), InvoiceStatus.DRAFT)).hasSize(1);
            assertThat(invoiceService.listByStatus(user.getId(), InvoiceStatus.SENT)).hasSize(1);
        }
    }

    @Nested
    class ReverseChargeAutoDetection {

        private static final List<LineItem> ZERO_VAT_ITEMS = List.of(
                new LineItem("Consulting", new BigDecimal("10"), new BigDecimal("150.00"), BigDecimal.ZERO));

        @Test
        void euB2B_autoDetectsReverseCharge() {
            Client euClient = clientRepository.save(new Client(user, "Austrian Corp",
                    IncomeStream.FREIBERUF, ClientType.B2B, "AT", "ATU12345678"));

            Invoice invoice = invoiceService.create(user, InvoiceStream.FREIBERUF, euClient.getId(),
                    LocalDate.of(2026, 3, 15), null, ZERO_VAT_ITEMS,
                    new BigDecimal("1500.00"), BigDecimal.ZERO, new BigDecimal("1500.00"),
                    null, null);

            assertThat(invoice.getVatTreatment()).isEqualTo(VatTreatment.REVERSE_CHARGE);
            assertThat(invoice.getNotes()).contains("Steuerschuldnerschaft des Leistungsempfängers");
        }

        @Test
        void nonEU_autoDetectsThirdCountry() {
            Client usClient = clientRepository.save(new Client(user, "US Corp",
                    IncomeStream.FREIBERUF, ClientType.B2B, "US", null));

            Invoice invoice = invoiceService.create(user, InvoiceStream.FREIBERUF, usClient.getId(),
                    LocalDate.of(2026, 3, 15), null, ZERO_VAT_ITEMS,
                    new BigDecimal("1500.00"), BigDecimal.ZERO, new BigDecimal("1500.00"),
                    null, null);

            assertThat(invoice.getVatTreatment()).isEqualTo(VatTreatment.THIRD_COUNTRY);
            assertThat(invoice.getNotes()).contains("§3a UStG");
        }

        @Test
        void deClient_autoDetectsRegular() {
            Invoice invoice = invoiceService.create(user, InvoiceStream.FREIBERUF, frClient.getId(),
                    LocalDate.of(2026, 3, 15), null, VALID_ITEMS,
                    new BigDecimal("1500.00"), new BigDecimal("285.00"), new BigDecimal("1785.00"),
                    null, null);

            assertThat(invoice.getVatTreatment()).isEqualTo(VatTreatment.REGULAR);
        }

        @Test
        void euB2C_autoDetectsRegular() {
            Client euB2cClient = clientRepository.save(new Client(user, "Italian Consumer",
                    IncomeStream.FREIBERUF, ClientType.B2C, "IT", null));

            Invoice invoice = invoiceService.create(user, InvoiceStream.FREIBERUF, euB2cClient.getId(),
                    LocalDate.of(2026, 3, 15), null, VALID_ITEMS,
                    new BigDecimal("1500.00"), new BigDecimal("285.00"), new BigDecimal("1785.00"),
                    null, null);

            assertThat(invoice.getVatTreatment()).isEqualTo(VatTreatment.REGULAR);
        }

        @Test
        void reverseCharge_autoAppendsNotice() {
            Client euClient = clientRepository.save(new Client(user, "French Corp",
                    IncomeStream.FREIBERUF, ClientType.B2B, "FR", "FR12345678901"));

            Invoice invoice = invoiceService.create(user, InvoiceStream.FREIBERUF, euClient.getId(),
                    LocalDate.of(2026, 3, 15), null, ZERO_VAT_ITEMS,
                    new BigDecimal("1000.00"), BigDecimal.ZERO, new BigDecimal("1000.00"),
                    VatTreatment.REVERSE_CHARGE, "Custom note");

            assertThat(invoice.getNotes()).contains("Custom note");
            assertThat(invoice.getNotes()).contains("Steuerschuldnerschaft des Leistungsempfängers");
        }

        @Test
        void reverseCharge_doesNotDuplicateNotice() {
            Client euClient = clientRepository.save(new Client(user, "Spanish Corp",
                    IncomeStream.FREIBERUF, ClientType.B2B, "ES", "ESB12345678"));

            String existingNotice = "Steuerschuldnerschaft des Leistungsempfängers (Reverse Charge, §13b UStG).";
            Invoice invoice = invoiceService.create(user, InvoiceStream.FREIBERUF, euClient.getId(),
                    LocalDate.of(2026, 3, 15), null, ZERO_VAT_ITEMS,
                    new BigDecimal("1000.00"), BigDecimal.ZERO, new BigDecimal("1000.00"),
                    VatTreatment.REVERSE_CHARGE, existingNotice);

            // Should not duplicate the notice
            int count = invoice.getNotes().split("Steuerschuldnerschaft").length - 1;
            assertThat(count).isEqualTo(1);
        }

        @Test
        void thirdCountry_withNonZeroVat_rejects() {
            Client usClient = clientRepository.save(new Client(user, "US Corp",
                    IncomeStream.FREIBERUF, ClientType.B2B, "US", null));

            assertThatThrownBy(() -> invoiceService.create(user, InvoiceStream.FREIBERUF, usClient.getId(),
                    LocalDate.of(2026, 3, 15), null, VALID_ITEMS,
                    new BigDecimal("1500.00"), new BigDecimal("285.00"), new BigDecimal("1785.00"),
                    VatTreatment.THIRD_COUNTRY, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Third country")
                    .hasMessageContaining("§3a UStG");
        }

        @Test
        void explicitTreatment_overridesAutoDetection() {
            // DE client but explicitly set to REVERSE_CHARGE (user override)
            Invoice invoice = invoiceService.create(user, InvoiceStream.FREIBERUF, frClient.getId(),
                    LocalDate.of(2026, 3, 15), null,
                    List.of(new LineItem("Service", new BigDecimal("1"), new BigDecimal("100.00"), BigDecimal.ZERO)),
                    new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("100.00"),
                    VatTreatment.REVERSE_CHARGE, null);

            assertThat(invoice.getVatTreatment()).isEqualTo(VatTreatment.REVERSE_CHARGE);
        }
    }

    @Nested
    class ZmReporting {

        private static final List<LineItem> ZERO_VAT_ITEMS = List.of(
                new LineItem("App Revenue", new BigDecimal("1"), new BigDecimal("5000.00"), BigDecimal.ZERO));

        @Test
        void euB2B_reverseCharge_isFlaggedZmReportable() {
            Client euClient = clientRepository.save(new Client(user, "Austrian Corp",
                    IncomeStream.FREIBERUF, ClientType.B2B, "AT", "ATU12345678"));

            Invoice invoice = invoiceService.create(user, InvoiceStream.FREIBERUF, euClient.getId(),
                    LocalDate.of(2026, 3, 15), null, ZERO_VAT_ITEMS,
                    new BigDecimal("5000.00"), BigDecimal.ZERO, new BigDecimal("5000.00"),
                    VatTreatment.REVERSE_CHARGE, null);

            assertThat(invoice.isZmReportable()).isTrue();
        }

        @Test
        void deClient_regular_notZmReportable() {
            Invoice invoice = createValidInvoice();
            assertThat(invoice.isZmReportable()).isFalse();
        }

        @Test
        void appleDistribution_flaggedForZm() {
            Client apple = clientRepository.save(new Client(user, "Apple Distribution International",
                    IncomeStream.FREIBERUF, ClientType.B2B, "IE", "IE9700053D"));

            Invoice invoice = invoiceService.create(user, InvoiceStream.FREIBERUF, apple.getId(),
                    LocalDate.of(2026, 3, 15), null, ZERO_VAT_ITEMS,
                    new BigDecimal("5000.00"), BigDecimal.ZERO, new BigDecimal("5000.00"),
                    VatTreatment.REVERSE_CHARGE, null);

            assertThat(invoice.isZmReportable()).isTrue();
        }

        @Test
        void googleIreland_flaggedForZm() {
            Client google = clientRepository.save(new Client(user, "Google Ireland Ltd",
                    IncomeStream.FREIBERUF, ClientType.B2B, "IE", "IE6388047V"));

            Invoice invoice = invoiceService.create(user, InvoiceStream.FREIBERUF, google.getId(),
                    LocalDate.of(2026, 3, 15), null, ZERO_VAT_ITEMS,
                    new BigDecimal("3000.00"), BigDecimal.ZERO, new BigDecimal("3000.00"),
                    VatTreatment.REVERSE_CHARGE, null);

            assertThat(invoice.isZmReportable()).isTrue();
        }

        @Test
        void nonEU_thirdCountry_notZmReportable() {
            Client usClient = clientRepository.save(new Client(user, "US Corp",
                    IncomeStream.FREIBERUF, ClientType.B2B, "US", null));

            Invoice invoice = invoiceService.create(user, InvoiceStream.FREIBERUF, usClient.getId(),
                    LocalDate.of(2026, 3, 15), null, ZERO_VAT_ITEMS,
                    new BigDecimal("5000.00"), BigDecimal.ZERO, new BigDecimal("5000.00"),
                    VatTreatment.THIRD_COUNTRY, null);

            assertThat(invoice.isZmReportable()).isFalse();
        }
    }

    private Invoice createValidInvoice() {
        return invoiceService.create(user, InvoiceStream.FREIBERUF, frClient.getId(),
                LocalDate.of(2026, 3, 15), null, VALID_ITEMS,
                new BigDecimal("1500.00"), new BigDecimal("285.00"), new BigDecimal("1785.00"),
                VatTreatment.REGULAR, null);
    }
}
