package com.trinity.financial.transaction.repository;

import com.trinity.financial.transaction.entity.FinancialTransactionEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialTransactionRepository
        extends JpaRepository<FinancialTransactionEntity, UUID> {
}
