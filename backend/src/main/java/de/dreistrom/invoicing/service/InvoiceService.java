package de.dreistrom.invoicing.service;

import de.dreistrom.audit.service.AuditLogService;
import de.dreistrom.common.controller.EntityNotFoundException;
import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.domain.IncomeStream;
import de.dreistrom.income.domain.Client;
import de.dreistrom.income.domain.IncomeEntry;
import de.dreistrom.income.repository.ClientRepository;
import de.dreistrom.income.repository.IncomeEntryRepository;
import de.dreistrom.invoicing.domain.Invoice;
import de.dreistrom.invoicing.domain.InvoiceStatus;
import de.dreistrom.invoicing.domain.InvoiceStream;
import de.dreistrom.invoicing.domain.LineItem;
import de.dreistrom.invoicing.domain.VatTreatment;
import de.dreistrom.invoicing.event.InvoiceCreated;
import de.dreistrom.invoicing.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Invoice lifecycle management with full §14 UStG field validation,
 * reverse charge auto-detection, and ZM reporting flags.
 * On creation, auto-creates a linked IncomeEntry in the correct stream.
 */
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceNumberGenerator numberGenerator;
    private final ReverseChargeService reverseChargeService;
    private final ClientRepository clientRepository;
    private final IncomeEntryRepository incomeEntryRepository;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Create a new invoice with full §14 UStG validation.
     * If vatTreatment is null, auto-detects from client properties.
     * Automatically generates the sequential invoice number, appends
     * reverse charge / §3a notices, sets ZM flag, and creates
     * a linked IncomeEntry for the gross total in the matching stream.
     */
    @Transactional
    public Invoice create(AppUser user, InvoiceStream streamType, Long clientId,
                          LocalDate invoiceDate, LocalDate dueDate,
                          List<LineItem> lineItems,
                          BigDecimal netTotal, BigDecimal vat, BigDecimal grossTotal,
                          VatTreatment vatTreatment, String notes) {

        Client client = resolveClient(clientId, user.getId(), streamType);

        // Auto-detect VatTreatment if not explicitly provided
        VatTreatment resolvedTreatment = vatTreatment != null
                ? vatTreatment
                : reverseChargeService.determineVatTreatment(client);

        // Auto-append VAT notice for reverse charge / third country / intra-EU
        String resolvedNotes = appendVatNoticeIfNeeded(notes, resolvedTreatment);

        // §14 UStG validation (with resolved treatment and notes)
        validateUStG14(client, invoiceDate, lineItems, netTotal, vat, grossTotal,
                resolvedTreatment, resolvedNotes);

        String number = numberGenerator.nextInvoiceNumber(streamType, invoiceDate.getYear());

        Invoice invoice = new Invoice(user, streamType, number, client, invoiceDate,
                lineItems, netTotal, vat, grossTotal, resolvedTreatment);

        // Set dueDate and/or notes via update if needed
        if (dueDate != null || resolvedNotes != null) {
            invoice.update(client, invoiceDate, dueDate, lineItems,
                    netTotal, vat, grossTotal, resolvedTreatment, resolvedNotes);
        }

        // ZM reporting flag
        invoice.markZmReportable(
                reverseChargeService.isZmReportable(client, resolvedTreatment));

        Invoice saved = invoiceRepository.save(invoice);

        // Auto-create linked IncomeEntry
        IncomeEntry incomeEntry = createLinkedIncomeEntry(user, streamType, saved);
        incomeEntryRepository.save(incomeEntry);

        InvoiceCreated event = new InvoiceCreated(saved);
        auditLogService.persist(event);
        eventPublisher.publishEvent(event);

        return saved;
    }

    /**
     * Update an existing DRAFT invoice. Only DRAFT invoices can be modified.
     */
    @Transactional
    public Invoice update(Long invoiceId, Long userId, Long clientId,
                          LocalDate invoiceDate, LocalDate dueDate,
                          List<LineItem> lineItems,
                          BigDecimal netTotal, BigDecimal vat, BigDecimal grossTotal,
                          VatTreatment vatTreatment, String notes) {

        Invoice invoice = getOwnedInvoice(invoiceId, userId);

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new IllegalArgumentException(
                    "Only DRAFT invoices can be updated, current status: " + invoice.getStatus());
        }

        Client client = resolveClient(clientId, userId, invoice.getStreamType());

        VatTreatment resolvedTreatment = vatTreatment != null
                ? vatTreatment
                : reverseChargeService.determineVatTreatment(client);

        String resolvedNotes = appendVatNoticeIfNeeded(notes, resolvedTreatment);

        validateUStG14(client, invoiceDate, lineItems, netTotal, vat, grossTotal,
                resolvedTreatment, resolvedNotes);

        invoice.update(client, invoiceDate, dueDate, lineItems,
                netTotal, vat, grossTotal, resolvedTreatment, resolvedNotes);

        invoice.markZmReportable(
                reverseChargeService.isZmReportable(client, resolvedTreatment));

        return invoice;
    }

    /**
     * Transition an invoice to a new status.
     * Only forward transitions are allowed: DRAFT→SENT→PAID, DRAFT/SENT→CANCELLED, SENT→OVERDUE.
     */
    @Transactional
    public Invoice updateStatus(Long invoiceId, Long userId, InvoiceStatus newStatus) {
        Invoice invoice = getOwnedInvoice(invoiceId, userId);
        InvoiceStatus current = invoice.getStatus();

        validateStatusTransition(current, newStatus);
        invoice.updateStatus(newStatus);

        return invoice;
    }

    @Transactional(readOnly = true)
    public Invoice getById(Long invoiceId, Long userId) {
        return getOwnedInvoice(invoiceId, userId);
    }

    @Transactional(readOnly = true)
    public List<Invoice> listAll(Long userId) {
        return invoiceRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<Invoice> listByStream(Long userId, InvoiceStream streamType) {
        return invoiceRepository.findByUserIdAndStreamType(userId, streamType);
    }

    @Transactional(readOnly = true)
    public List<Invoice> listByStatus(Long userId, InvoiceStatus status) {
        return invoiceRepository.findByUserIdAndStatus(userId, status);
    }

    /**
     * Delete an invoice. Only DRAFT invoices can be deleted.
     */
    @Transactional
    public void delete(Long invoiceId, Long userId) {
        Invoice invoice = getOwnedInvoice(invoiceId, userId);

        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new IllegalArgumentException(
                    "Only DRAFT invoices can be deleted, current status: " + invoice.getStatus());
        }

        invoiceRepository.delete(invoice);
    }

    // ── §14 UStG validation ──────────────────────────────────────────────

    /**
     * Validates required fields per §14 Abs. 4 UStG:
     * 1. Recipient (Client) with name
     * 2. Invoice date
     * 3. Service description (line items)
     * 4. Net amount, VAT rate per item, VAT total, gross total
     * 5. Kleinunternehmer: §19 UStG notice required, VAT must be zero
     * 6. Reverse charge: VAT must be zero
     * 7. Intra-EU: client must have USt-IdNr
     * 8. Third country: VAT must be zero (§3a UStG)
     */
    private void validateUStG14(Client client, LocalDate invoiceDate,
                                 List<LineItem> lineItems,
                                 BigDecimal netTotal, BigDecimal vat, BigDecimal grossTotal,
                                 VatTreatment vatTreatment, String notes) {

        List<String> errors = new ArrayList<>();

        // §14 Abs. 4 Nr. 1: Recipient
        if (client == null) {
            errors.add("Client (Leistungsempfänger) is required per §14 Abs. 4 Nr. 1 UStG");
        }

        // §14 Abs. 4 Nr. 4: Invoice date
        if (invoiceDate == null) {
            errors.add("Invoice date (Rechnungsdatum) is required per §14 Abs. 4 Nr. 4 UStG");
        }

        // §14 Abs. 4 Nr. 5: Service description via line items
        if (lineItems == null || lineItems.isEmpty()) {
            errors.add("At least one line item (Leistungsbeschreibung) is required per §14 Abs. 4 Nr. 5 UStG");
        } else {
            for (int i = 0; i < lineItems.size(); i++) {
                LineItem item = lineItems.get(i);
                if (item.description() == null || item.description().isBlank()) {
                    errors.add("Line item " + (i + 1) + ": description is required");
                }
                if (item.quantity() == null || item.quantity().compareTo(BigDecimal.ZERO) <= 0) {
                    errors.add("Line item " + (i + 1) + ": quantity must be positive");
                }
                if (item.unitPrice() == null || item.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
                    errors.add("Line item " + (i + 1) + ": unit price must not be negative");
                }
                if (item.vatRate() == null || item.vatRate().compareTo(BigDecimal.ZERO) < 0) {
                    errors.add("Line item " + (i + 1) + ": VAT rate must not be negative");
                }
            }
        }

        // §14 Abs. 4 Nr. 7/8: Amounts
        if (netTotal == null || netTotal.compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Net total (Entgelt) is required and must not be negative per §14 Abs. 4 Nr. 7 UStG");
        }
        if (vat == null || vat.compareTo(BigDecimal.ZERO) < 0) {
            errors.add("VAT amount (Steuerbetrag) must not be negative per §14 Abs. 4 Nr. 8 UStG");
        }
        if (grossTotal == null || grossTotal.compareTo(BigDecimal.ZERO) < 0) {
            errors.add("Gross total must not be negative");
        }

        // Kleinunternehmer §19 UStG validation
        if (vatTreatment == VatTreatment.SMALL_BUSINESS) {
            if (vat != null && vat.compareTo(BigDecimal.ZERO) != 0) {
                errors.add("Kleinunternehmer invoice: VAT must be 0 per §19 Abs. 1 UStG");
            }
            if (lineItems != null) {
                for (int i = 0; i < lineItems.size(); i++) {
                    if (lineItems.get(i).vatRate() != null
                            && lineItems.get(i).vatRate().compareTo(BigDecimal.ZERO) != 0) {
                        errors.add("Kleinunternehmer invoice: line item " + (i + 1)
                                + " VAT rate must be 0");
                    }
                }
            }
            if (notes == null || !notes.contains("\u00a719")) {
                errors.add("Kleinunternehmer invoice must include §19 UStG notice in notes "
                        + "(e.g. 'Gemäß §19 UStG wird keine Umsatzsteuer berechnet')");
            }
        }

        // Reverse charge validation
        if (vatTreatment == VatTreatment.REVERSE_CHARGE) {
            if (vat != null && vat.compareTo(BigDecimal.ZERO) != 0) {
                errors.add("Reverse charge invoice: VAT must be 0 (Steuerschuldnerschaft des Leistungsempfängers)");
            }
        }

        // Intra-EU validation
        if (vatTreatment == VatTreatment.INTRA_EU) {
            if (vat != null && vat.compareTo(BigDecimal.ZERO) != 0) {
                errors.add("Intra-EU invoice: VAT must be 0 (innergemeinschaftliche Lieferung)");
            }
            if (client != null && (client.getUstIdNr() == null || client.getUstIdNr().isBlank())) {
                errors.add("Intra-EU invoice: client must have USt-IdNr per §14a Abs. 3 UStG");
            }
        }

        // Third country (Drittland) §3a UStG validation
        if (vatTreatment == VatTreatment.THIRD_COUNTRY) {
            if (vat != null && vat.compareTo(BigDecimal.ZERO) != 0) {
                errors.add("Third country invoice: VAT must be 0 per §3a UStG (Leistungsort im Drittland)");
            }
        }

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(
                    "Invoice validation failed: " + String.join("; ", errors));
        }
    }

    private void validateStatusTransition(InvoiceStatus current, InvoiceStatus target) {
        boolean valid = switch (current) {
            case DRAFT -> target == InvoiceStatus.SENT || target == InvoiceStatus.CANCELLED;
            case SENT -> target == InvoiceStatus.PAID || target == InvoiceStatus.OVERDUE
                    || target == InvoiceStatus.CANCELLED;
            case OVERDUE -> target == InvoiceStatus.PAID || target == InvoiceStatus.CANCELLED;
            case PAID, CANCELLED -> false;
        };

        if (!valid) {
            throw new IllegalArgumentException(
                    "Invalid status transition: " + current + " \u2192 " + target);
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────

    /**
     * Auto-append the appropriate VAT notice to notes if not already present.
     */
    private String appendVatNoticeIfNeeded(String notes, VatTreatment vatTreatment) {
        String notice = reverseChargeService.getVatNotice(vatTreatment);
        if (notice == null) {
            return notes;
        }
        if (notes != null && notes.contains(notice)) {
            return notes;
        }
        if (notes == null || notes.isBlank()) {
            return notice;
        }
        return notes + "\n" + notice;
    }

    private Client resolveClient(Long clientId, Long userId, InvoiceStream streamType) {
        if (clientId == null) {
            return null;
        }
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Client", clientId));
        if (!client.getUser().getId().equals(userId)) {
            throw new EntityNotFoundException("Client", clientId);
        }
        IncomeStream expectedStream = toIncomeStream(streamType);
        if (client.getStreamType() != expectedStream) {
            throw new IllegalArgumentException(
                    "Client stream type " + client.getStreamType()
                    + " does not match invoice stream type " + streamType);
        }
        return client;
    }

    private IncomeEntry createLinkedIncomeEntry(AppUser user, InvoiceStream streamType,
                                                 Invoice invoice) {
        IncomeStream incomeStream = toIncomeStream(streamType);
        IncomeEntry entry = new IncomeEntry(user, incomeStream, invoice.getGrossTotal(),
                invoice.getInvoiceDate(), "Rechnung " + invoice.getNumber(),
                invoice.getClient(), "Auto-erstellt aus Rechnung " + invoice.getNumber());
        entry.linkInvoice(invoice.getId());
        return entry;
    }

    private static IncomeStream toIncomeStream(InvoiceStream streamType) {
        return switch (streamType) {
            case FREIBERUF -> IncomeStream.FREIBERUF;
            case GEWERBE -> IncomeStream.GEWERBE;
        };
    }

    private Invoice getOwnedInvoice(Long invoiceId, Long userId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new EntityNotFoundException("Invoice", invoiceId));
        if (!invoice.getUser().getId().equals(userId)) {
            throw new EntityNotFoundException("Invoice", invoiceId);
        }
        return invoice;
    }
}
