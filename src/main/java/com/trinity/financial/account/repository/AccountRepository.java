package com.trinity.financial.account.repository;

import com.trinity.financial.account.entity.AccountEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AccountRepository
        extends JpaRepository<AccountEntity, UUID>, JpaSpecificationExecutor<AccountEntity> {

    boolean existsByCustomerId(UUID customerId);

    boolean existsByAccountNumber(String accountNumber);

    Optional<AccountEntity> findByAccountNumber(String accountNumber);
}
