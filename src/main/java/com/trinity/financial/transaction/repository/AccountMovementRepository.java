package com.trinity.financial.transaction.repository;

import com.trinity.financial.transaction.entity.AccountMovementEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountMovementRepository extends JpaRepository<AccountMovementEntity, UUID> {

    List<AccountMovementEntity> findByFinancialTransactionIdOrderByCreatedAtAscIdAsc(
            UUID financialTransactionId);

    Page<AccountMovementEntity> findByAccountId(UUID accountId, Pageable pageable);

    Page<AccountMovementEntity> findByAccountIdAndCreatedAtGreaterThanEqual(
            UUID accountId,
            LocalDateTime startDate,
            Pageable pageable);

    Page<AccountMovementEntity> findByAccountIdAndCreatedAtLessThanEqual(
            UUID accountId,
            LocalDateTime endDate,
            Pageable pageable);

    Page<AccountMovementEntity> findByAccountIdAndCreatedAtBetween(
            UUID accountId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable);
}
