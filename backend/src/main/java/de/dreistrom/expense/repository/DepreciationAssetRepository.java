package de.dreistrom.expense.repository;

import de.dreistrom.expense.domain.DepreciationAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DepreciationAssetRepository extends JpaRepository<DepreciationAsset, Long> {

    List<DepreciationAsset> findByUserId(Long userId);

    List<DepreciationAsset> findByExpenseEntryId(Long expenseEntryId);

    /**
     * Sum annual AfA cents for a given user across all assets.
     */
    @Query(value = "SELECT SUM(annual_afa_cents) FROM depreciation_asset " +
                   "WHERE user_id = :userId",
           nativeQuery = true)
    Long sumAnnualAfaCentsByUserId(@Param("userId") Long userId);
}
