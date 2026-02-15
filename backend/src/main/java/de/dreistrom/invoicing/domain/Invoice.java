package de.dreistrom.invoicing.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.domain.MoneyConverter;
import de.dreistrom.income.domain.Client;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "invoice")
@Getter
@NoArgsConstructor
public class Invoice {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Enumerated(EnumType.STRING)
    @Column(name = "stream_type", nullable = false)
    private InvoiceStream streamType;

    @Column(nullable = false, length = 50)
    private String number;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "invoice_date", nullable = false)
    private LocalDate invoiceDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "line_items", nullable = false, columnDefinition = "JSON")
    private String lineItemsJson;

    @Convert(converter = MoneyConverter.class)
    @Column(name = "net_total_cents", nullable = false)
    private BigDecimal netTotal;

    @Convert(converter = MoneyConverter.class)
    @Column(name = "vat_cents", nullable = false)
    private BigDecimal vat;

    @Convert(converter = MoneyConverter.class)
    @Column(name = "gross_total_cents", nullable = false)
    private BigDecimal grossTotal;

    @Column(nullable = false, length = 3)
    private String currency = "EUR";

    @Enumerated(EnumType.STRING)
    @Column(name = "vat_treatment", nullable = false)
    private VatTreatment vatTreatment = VatTreatment.REGULAR;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "zm_reportable", nullable = false)
    private boolean zmReportable = false;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false,
            columnDefinition = "TIMESTAMP(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private Instant updatedAt;

    public Invoice(AppUser user, InvoiceStream streamType, String number, Client client,
                   LocalDate invoiceDate, List<LineItem> lineItems,
                   BigDecimal netTotal, BigDecimal vat, BigDecimal grossTotal,
                   VatTreatment vatTreatment) {
        this.user = user;
        this.streamType = streamType;
        this.number = number;
        this.client = client;
        this.invoiceDate = invoiceDate;
        setLineItems(lineItems);
        this.netTotal = netTotal;
        this.vat = vat;
        this.grossTotal = grossTotal;
        this.vatTreatment = vatTreatment;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public List<LineItem> getLineItems() {
        try {
            return MAPPER.readValue(lineItemsJson, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize line items", e);
        }
    }

    public void setLineItems(List<LineItem> lineItems) {
        try {
            this.lineItemsJson = MAPPER.writeValueAsString(lineItems);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize line items", e);
        }
    }

    public void update(Client client, LocalDate invoiceDate, LocalDate dueDate,
                       List<LineItem> lineItems, BigDecimal netTotal,
                       BigDecimal vat, BigDecimal grossTotal,
                       VatTreatment vatTreatment, String notes) {
        this.client = client;
        this.invoiceDate = invoiceDate;
        this.dueDate = dueDate;
        setLineItems(lineItems);
        this.netTotal = netTotal;
        this.vat = vat;
        this.grossTotal = grossTotal;
        this.vatTreatment = vatTreatment;
        this.notes = notes;
        this.updatedAt = Instant.now();
    }

    public void updateStatus(InvoiceStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public void markZmReportable(boolean zmReportable) {
        this.zmReportable = zmReportable;
    }
}
