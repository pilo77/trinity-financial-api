package com.trinity.financial.account.repository;

import com.trinity.financial.account.entity.AccountEntity;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository
        extends JpaRepository<AccountEntity, UUID>, JpaSpecificationExecutor<AccountEntity> {

    boolean existsByCustomerId(UUID customerId);

    boolean existsByAccountNumber(String accountNumber);

    Optional<AccountEntity> findByAccountNumber(String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from AccountEntity account where account.accountNumber = :accountNumber")
    Optional<AccountEntity> findByAccountNumberForUpdate(
            @Param("accountNumber") String accountNumber);
}
