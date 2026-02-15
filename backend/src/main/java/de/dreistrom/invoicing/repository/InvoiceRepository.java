package de.dreistrom.invoicing.repository;

import de.dreistrom.invoicing.domain.Invoice;
import de.dreistrom.invoicing.domain.InvoiceStatus;
import de.dreistrom.invoicing.domain.InvoiceStream;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByUserId(Long userId);

    List<Invoice> findByUserIdAndStreamType(Long userId, InvoiceStream streamType);

    List<Invoice> findByUserIdAndStatus(Long userId, InvoiceStatus status);

    List<Invoice> findByUserIdAndInvoiceDateBetween(Long userId, LocalDate from, LocalDate to);

    List<Invoice> findByUserIdAndClientId(Long userId, Long clientId);

    Optional<Invoice> findByNumber(String number);

    @Query(value = "SELECT SUM(vat_cents) FROM invoice " +
                   "WHERE user_id = :userId " +
                   "AND stream_type = :streamType " +
                   "AND status != 'CANCELLED' " +
                   "AND invoice_date BETWEEN :from AND :to",
           nativeQuery = true)
    Long sumVatCentsByStreamAndDateRange(@Param("userId") Long userId,
                                        @Param("streamType") String streamType,
                                        @Param("from") LocalDate from,
                                        @Param("to") LocalDate to);
}
