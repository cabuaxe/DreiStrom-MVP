package de.dreistrom.socialinsurance.repository;

import de.dreistrom.socialinsurance.domain.SocialInsuranceEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SocialInsuranceEntryRepository extends JpaRepository<SocialInsuranceEntry, Long> {

    List<SocialInsuranceEntry> findByUserIdAndYearOrderByMonthAsc(Long userId, short year);

    Optional<SocialInsuranceEntry> findByUserIdAndYearAndMonth(Long userId, short year, short month);
}
