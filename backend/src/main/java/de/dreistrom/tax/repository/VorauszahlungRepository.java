package de.dreistrom.tax.repository;

import de.dreistrom.tax.domain.Vorauszahlung;
import de.dreistrom.tax.domain.VorauszahlungStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface VorauszahlungRepository extends JpaRepository<Vorauszahlung, Long> {

    List<Vorauszahlung> findByUserIdAndYearOrderByQuarter(Long userId, short year);

    Optional<Vorauszahlung> findByUserIdAndYearAndQuarter(Long userId, short year, short quarter);

    List<Vorauszahlung> findByStatusAndDueDateBefore(VorauszahlungStatus status, LocalDate date);
}
