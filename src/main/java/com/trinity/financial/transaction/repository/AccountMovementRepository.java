package com.trinity.financial.transaction.repository;

import com.trinity.financial.transaction.entity.AccountMovementEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountMovementRepository extends JpaRepository<AccountMovementEntity, UUID> {

    List<AccountMovementEntity> findByFinancialTransactionIdOrderByCreatedAtAscIdAsc(
            UUID financialTransactionId);

    @Query(
        value = """
            select movement from AccountMovementEntity movement
            where movement.account.id = :accountId
              and (:startDate is null or movement.createdAt >= :startDate)
              and (:endDate is null or movement.createdAt <= :endDate)
            """,
        countQuery = """
            select count(movement) from AccountMovementEntity movement
            where movement.account.id = :accountId
              and (:startDate is null or movement.createdAt >= :startDate)
              and (:endDate is null or movement.createdAt <= :endDate)
            """)
    Page<AccountMovementEntity> findStatementMovements(
        @Param("accountId") UUID accountId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable);
}
