package com.trinity.financial.transaction.repository;

import com.trinity.financial.transaction.entity.AccountMovementEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountMovementRepository extends JpaRepository<AccountMovementEntity, UUID> {

    List<AccountMovementEntity> findByFinancialTransactionIdOrderByCreatedAtAscIdAsc(
            UUID financialTransactionId);
}
