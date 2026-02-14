package de.dreistrom.income.repository;

import de.dreistrom.common.domain.IncomeStream;
import de.dreistrom.income.domain.Client;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClientRepository extends JpaRepository<Client, Long> {

    List<Client> findByUserIdAndActiveTrue(Long userId);

    List<Client> findByUserIdAndStreamType(Long userId, IncomeStream streamType);

    List<Client> findByUserIdAndStreamTypeAndActiveTrue(Long userId, IncomeStream streamType);

    List<Client> findByUserId(Long userId);
}
