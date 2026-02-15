package de.dreistrom.invoicing.mapper;

import de.dreistrom.income.domain.Client;
import de.dreistrom.income.dto.ClientSummary;
import de.dreistrom.invoicing.domain.Invoice;
import de.dreistrom.invoicing.domain.LineItem;
import de.dreistrom.invoicing.dto.InvoiceResponse;
import de.dreistrom.invoicing.dto.LineItemRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface InvoiceMapper {

    @Mapping(target = "client", source = "client", qualifiedByName = "clientToSummary")
    @Mapping(target = "lineItems", expression = "java(invoice.getLineItems())")
    InvoiceResponse toResponse(Invoice invoice);

    List<InvoiceResponse> toResponseList(List<Invoice> invoices);

    default LineItem toLineItem(LineItemRequest request) {
        return new LineItem(request.description(), request.quantity(),
                request.unitPrice(), request.vatRate());
    }

    default List<LineItem> toLineItems(List<LineItemRequest> requests) {
        if (requests == null) {
            return null;
        }
        return requests.stream().map(this::toLineItem).toList();
    }

    @Named("clientToSummary")
    default ClientSummary clientToSummary(Client client) {
        if (client == null) {
            return null;
        }
        return new ClientSummary(client.getId(), client.getName(), client.getClientType());
    }
}
