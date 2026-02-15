package de.dreistrom.integration.repository;

import de.dreistrom.integration.domain.AppStorePayout;
import de.dreistrom.integration.domain.PayoutPlatform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface AppStorePayoutRepository extends JpaRepository<AppStorePayout, Long> {

    List<AppStorePayout> findByUserIdAndPlatformAndReportDateBetweenOrderByReportDateAsc(
            Long userId, PayoutPlatform platform, LocalDate from, LocalDate to);

    List<AppStorePayout> findByImportBatchId(String batchId);

    boolean existsByUserIdAndPlatformAndReportDateAndProductIdAndRegionAndImportBatchId(
            Long userId, PayoutPlatform platform, LocalDate reportDate,
            String productId, String region, String importBatchId);

    @Query("SELECT COALESCE(SUM(p.netRevenue), 0) FROM AppStorePayout p " +
           "WHERE p.user.id = :userId AND p.platform = :platform " +
           "AND p.reportDate BETWEEN :from AND :to")
    BigDecimal sumNetRevenue(Long userId, PayoutPlatform platform,
                             LocalDate from, LocalDate to);
}
