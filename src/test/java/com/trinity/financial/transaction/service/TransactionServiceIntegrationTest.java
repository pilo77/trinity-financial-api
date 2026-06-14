package com.trinity.financial.transaction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.trinity.financial.account.entity.AccountEntity;
import com.trinity.financial.account.entity.AccountStatus;
import com.trinity.financial.account.entity.AccountType;
import com.trinity.financial.account.repository.AccountRepository;
import com.trinity.financial.customer.entity.CustomerEntity;
import com.trinity.financial.customer.repository.CustomerRepository;
import com.trinity.financial.transaction.dto.TransferRequest;
import com.trinity.financial.transaction.repository.AccountMovementRepository;
import com.trinity.financial.transaction.repository.FinancialTransactionRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@ActiveProfiles("test")
@SpringBootTest
class TransactionServiceIntegrationTest {

    @Autowired
    private TransactionService transactionService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private FinancialTransactionRepository transactionRepository;
    @Autowired
    private EntityManager entityManager;
    @MockitoBean
    private AccountMovementRepository movementRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        customerRepository.deleteAll();
    }

    @Test
    void rollsBackBalancesAndTransactionWhenMovementPersistenceFails() {
        LocalDateTime now = LocalDateTime.of(2026, 6, 14, 17, 0);
        CustomerEntity customer = customerRepository.saveAndFlush(new CustomerEntity(
                UUID.randomUUID(), "CC", "900001", "Carlos", "Villamil",
                "rollback@example.com", null, LocalDate.of(1990, 1, 1), now, now));
        accountRepository.saveAndFlush(account(
                "3300000001", AccountType.CHECKING, customer, "20.00", now));
        accountRepository.saveAndFlush(account(
                "5300000001", AccountType.SAVINGS, customer, "100.00", now));
        when(movementRepository.saveAllAndFlush(any()))
                .thenThrow(new IllegalStateException("forced movement failure"));

        assertThatThrownBy(() -> transactionService.transfer(
                new TransferRequest(
                        "5300000001", "3300000001", new BigDecimal("30.00"), null)))
                .isInstanceOf(IllegalStateException.class);

        entityManager.clear();
        AccountEntity source = accountRepository.findByAccountNumber("5300000001").orElseThrow();
        AccountEntity destination =
                accountRepository.findByAccountNumber("3300000001").orElseThrow();
        assertThat(source.getBalance()).isEqualByComparingTo("100.00");
        assertThat(source.getAvailableBalance()).isEqualByComparingTo("100.00");
        assertThat(destination.getBalance()).isEqualByComparingTo("20.00");
        assertThat(destination.getAvailableBalance()).isEqualByComparingTo("20.00");
        assertThat(transactionRepository.count()).isZero();
    }

    private AccountEntity account(
            String number,
            AccountType type,
            CustomerEntity customer,
            String balance,
            LocalDateTime now) {
        BigDecimal amount = new BigDecimal(balance);
        return new AccountEntity(
                UUID.randomUUID(), number, type, AccountStatus.ACTIVE,
                amount, amount, false, customer, now, now);
    }
}
