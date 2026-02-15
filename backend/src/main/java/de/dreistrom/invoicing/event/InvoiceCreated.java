package de.dreistrom.invoicing.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.dreistrom.audit.event.DomainEvent;
import de.dreistrom.invoicing.domain.Invoice;

import java.math.BigDecimal;

public class InvoiceCreated extends DomainEvent {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String streamType;
    private final String number;
    private final BigDecimal netTotal;
    private final BigDecimal vat;
    private final BigDecimal grossTotal;
    private final String vatTreatment;
    private final String clientName;

    public InvoiceCreated(Invoice invoice) {
        super("Invoice", invoice.getId(), "INVOICE_CREATED");
        this.streamType = invoice.getStreamType().name();
        this.number = invoice.getNumber();
        this.netTotal = invoice.getNetTotal();
        this.vat = invoice.getVat();
        this.grossTotal = invoice.getGrossTotal();
        this.vatTreatment = invoice.getVatTreatment().name();
        this.clientName = invoice.getClient().getName();
    }

    @Override
    public String toJsonPayload() {
        try {
            ObjectNode node = MAPPER.createObjectNode();
            node.put("streamType", streamType);
            node.put("number", number);
            node.put("netTotal", netTotal.toPlainString());
            node.put("vat", vat.toPlainString());
            node.put("grossTotal", grossTotal.toPlainString());
            node.put("vatTreatment", vatTreatment);
            node.put("clientName", clientName);
            return MAPPER.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize event payload", e);
        }
    }
}
