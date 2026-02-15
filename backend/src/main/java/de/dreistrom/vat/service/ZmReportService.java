package de.dreistrom.vat.service;

import de.dreistrom.invoicing.domain.Invoice;
import de.dreistrom.invoicing.repository.InvoiceRepository;
import de.dreistrom.vat.dto.ZmReport;
import de.dreistrom.vat.dto.ZmReportLine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generates Zusammenfassende Meldung (ZM) reports per §18a UStG.
 * Aggregates ZM-reportable invoices by country + USt-IdNr for a given period.
 */
@Service
@RequiredArgsConstructor
public class ZmReportService {

    private final InvoiceRepository invoiceRepository;

    /**
     * Generate a ZM report aggregating EU B2B reverse charge invoices
     * for a reporting period.
     */
    @Transactional(readOnly = true)
    public ZmReport generate(Long userId, LocalDate from, LocalDate to) {
        List<Invoice> zmInvoices = invoiceRepository
                .findByUserIdAndZmReportableTrueAndInvoiceDateBetween(userId, from, to);

        // Group by country + USt-IdNr key
        Map<String, List<Invoice>> grouped = zmInvoices.stream()
                .collect(Collectors.groupingBy(inv -> groupKey(inv)));

        List<ZmReportLine> lines = grouped.entrySet().stream()
                .map(entry -> {
                    List<Invoice> invoices = entry.getValue();
                    Invoice first = invoices.getFirst();
                    BigDecimal totalNet = invoices.stream()
                            .map(Invoice::getNetTotal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new ZmReportLine(
                            first.getClient().getCountry(),
                            first.getClient().getUstIdNr(),
                            first.getClient().getName(),
                            totalNet,
                            invoices.size());
                })
                .sorted(Comparator.comparing(ZmReportLine::country)
                        .thenComparing(ZmReportLine::ustIdNr))
                .toList();

        BigDecimal totalNet = lines.stream()
                .map(ZmReportLine::netTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ZmReport(from, to, lines, totalNet, zmInvoices.size());
    }

    private String groupKey(Invoice inv) {
        String country = inv.getClient().getCountry();
        String ustIdNr = inv.getClient().getUstIdNr();
        return country + "|" + (ustIdNr != null ? ustIdNr : "");
    }
}
