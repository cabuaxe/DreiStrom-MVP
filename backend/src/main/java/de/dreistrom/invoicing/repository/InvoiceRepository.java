package de.dreistrom.invoicing.repository;

import de.dreistrom.invoicing.domain.Invoice;
import de.dreistrom.invoicing.domain.InvoiceStatus;
import de.dreistrom.invoicing.domain.InvoiceStream;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    List<Invoice> findByUserId(Long userId);

    List<Invoice> findByUserIdAndStreamType(Long userId, InvoiceStream streamType);

    List<Invoice> findByUserIdAndStatus(Long userId, InvoiceStatus status);

    Optional<Invoice> findByNumber(String number);
}
