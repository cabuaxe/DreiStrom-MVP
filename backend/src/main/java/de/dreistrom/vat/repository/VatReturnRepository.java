package de.dreistrom.vat.repository;

import de.dreistrom.vat.domain.PeriodType;
import de.dreistrom.vat.domain.VatReturn;
import de.dreistrom.vat.domain.VatReturnStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VatReturnRepository extends JpaRepository<VatReturn, Long> {

    List<VatReturn> findByUserIdAndYear(Long userId, short year);

    List<VatReturn> findByUserIdAndYearAndPeriodType(Long userId, short year, PeriodType periodType);

    Optional<VatReturn> findByUserIdAndYearAndPeriodTypeAndPeriodNumber(
            Long userId, short year, PeriodType periodType, short periodNumber);

    List<VatReturn> findByUserIdAndStatus(Long userId, VatReturnStatus status);
}
