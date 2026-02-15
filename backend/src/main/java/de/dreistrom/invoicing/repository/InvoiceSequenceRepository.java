package de.dreistrom.invoicing.repository;

import de.dreistrom.invoicing.domain.InvoiceSequence;
import de.dreistrom.invoicing.domain.InvoiceSequenceId;
import de.dreistrom.invoicing.domain.InvoiceStream;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InvoiceSequenceRepository extends JpaRepository<InvoiceSequence, InvoiceSequenceId> {

    /**
     * SELECT ... FOR UPDATE — pessimistic write lock to prevent duplicate
     * invoice numbers under concurrent access.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM InvoiceSequence s WHERE s.streamType = :streamType AND s.fiscalYear = :fiscalYear")
    Optional<InvoiceSequence> findForUpdate(
            @Param("streamType") InvoiceStream streamType,
            @Param("fiscalYear") int fiscalYear);
}
