package de.dreistrom.invoicing.controller;

import de.dreistrom.common.Idempotent;
import de.dreistrom.common.domain.AppUser;
import de.dreistrom.common.service.AppUserDetails;
import de.dreistrom.invoicing.domain.Invoice;
import de.dreistrom.invoicing.domain.InvoiceStatus;
import de.dreistrom.invoicing.domain.InvoiceStream;
import de.dreistrom.invoicing.domain.LineItem;
import de.dreistrom.invoicing.dto.CreateInvoiceRequest;
import de.dreistrom.invoicing.dto.InvoiceResponse;
import de.dreistrom.invoicing.dto.UpdateInvoiceRequest;
import de.dreistrom.invoicing.dto.UpdateInvoiceStatusRequest;
import de.dreistrom.invoicing.mapper.InvoiceMapper;
import de.dreistrom.invoicing.service.InvoicePdfService;
import de.dreistrom.invoicing.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoices", description = "Invoice management with §14 UStG validation and PDF generation")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoicePdfService invoicePdfService;
    private final InvoiceMapper invoiceMapper;
    private final EntityManager entityManager;

    @PostMapping
    @Idempotent
    @Operation(summary = "Create a new invoice",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Invoice created",
                            headers = @Header(name = "Location", description = "URI of the created invoice")),
                    @ApiResponse(responseCode = "400", description = "Validation error (§14 UStG)"),
                    @ApiResponse(responseCode = "404", description = "Client not found"),
                    @ApiResponse(responseCode = "409", description = "Duplicate idempotency key")
            })
    public ResponseEntity<InvoiceResponse> create(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @Valid @RequestBody CreateInvoiceRequest request) {

        AppUser user = entityManager.getReference(AppUser.class, userDetails.getId());
        List<LineItem> lineItems = invoiceMapper.toLineItems(request.lineItems());

        Invoice saved = invoiceService.create(
                user, request.streamType(), request.clientId(),
                request.invoiceDate(), request.dueDate(), lineItems,
                request.netTotal(), request.vat(), request.grossTotal(),
                request.vatTreatment(), request.notes());

        InvoiceResponse response = invoiceMapper.toResponse(saved);
        return ResponseEntity
                .created(URI.create("/api/v1/invoices/" + saved.getId()))
                .body(response);
    }

    @GetMapping
    @Operation(summary = "List invoices with optional filters",
            responses = @ApiResponse(responseCode = "200", description = "List of invoices"))
    public ResponseEntity<List<InvoiceResponse>> list(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @RequestParam(required = false) InvoiceStream streamType,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {

        Long userId = userDetails.getId();
        List<Invoice> invoices;

        if (streamType != null) {
            invoices = invoiceService.listByStream(userId, streamType);
        } else if (status != null) {
            invoices = invoiceService.listByStatus(userId, status);
        } else if (clientId != null) {
            invoices = invoiceService.listByClient(userId, clientId);
        } else if (fromDate != null && toDate != null) {
            invoices = invoiceService.listByDateRange(userId, fromDate, toDate);
        } else {
            invoices = invoiceService.listAll(userId);
        }

        return ResponseEntity.ok(invoiceMapper.toResponseList(invoices));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an invoice by ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Invoice found"),
                    @ApiResponse(responseCode = "404", description = "Invoice not found")
            })
    public ResponseEntity<InvoiceResponse> getById(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable Long id) {

        Invoice invoice = invoiceService.getById(id, userDetails.getId());
        return ResponseEntity.ok(invoiceMapper.toResponse(invoice));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a DRAFT invoice",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Invoice updated"),
                    @ApiResponse(responseCode = "400", description = "Validation error or not DRAFT"),
                    @ApiResponse(responseCode = "404", description = "Invoice not found")
            })
    public ResponseEntity<InvoiceResponse> update(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody UpdateInvoiceRequest request) {

        List<LineItem> lineItems = invoiceMapper.toLineItems(request.lineItems());

        Invoice updated = invoiceService.update(
                id, userDetails.getId(), request.clientId(),
                request.invoiceDate(), request.dueDate(), lineItems,
                request.netTotal(), request.vat(), request.grossTotal(),
                request.vatTreatment(), request.notes());

        return ResponseEntity.ok(invoiceMapper.toResponse(updated));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Transition invoice status",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Status updated"),
                    @ApiResponse(responseCode = "400", description = "Invalid status transition"),
                    @ApiResponse(responseCode = "404", description = "Invoice not found")
            })
    public ResponseEntity<InvoiceResponse> updateStatus(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody UpdateInvoiceStatusRequest request) {

        Invoice updated = invoiceService.updateStatus(id, userDetails.getId(), request.status());
        return ResponseEntity.ok(invoiceMapper.toResponse(updated));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a DRAFT invoice",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Invoice deleted"),
                    @ApiResponse(responseCode = "400", description = "Not a DRAFT invoice"),
                    @ApiResponse(responseCode = "404", description = "Invoice not found")
            })
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable Long id) {

        invoiceService.delete(id, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/pdf")
    @Operation(summary = "Generate PDF for an invoice with all §14 UStG fields",
            responses = {
                    @ApiResponse(responseCode = "200", description = "PDF generated"),
                    @ApiResponse(responseCode = "404", description = "Invoice not found")
            })
    public ResponseEntity<byte[]> generatePdf(
            @AuthenticationPrincipal AppUserDetails userDetails,
            @PathVariable Long id) {

        Invoice invoice = invoiceService.getById(id, userDetails.getId());
        byte[] pdf = invoicePdfService.generatePdf(invoice);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + invoice.getNumber() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
