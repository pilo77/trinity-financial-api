package com.trinity.financial.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.trinity.financial.account.dto.AccountStatementResponse;
import com.trinity.financial.account.entity.AccountEntity;
import com.trinity.financial.account.entity.AccountStatus;
import com.trinity.financial.account.entity.AccountType;
import com.trinity.financial.account.repository.AccountRepository;
import com.trinity.financial.customer.entity.CustomerEntity;
import com.trinity.financial.shared.exception.InvalidRequestException;
import com.trinity.financial.shared.exception.ResourceNotFoundException;
import com.trinity.financial.transaction.entity.AccountMovementEntity;
import com.trinity.financial.transaction.entity.FinancialTransactionEntity;
import com.trinity.financial.transaction.entity.MovementType;
import com.trinity.financial.transaction.entity.TransactionStatus;
import com.trinity.financial.transaction.entity.TransactionType;
import com.trinity.financial.transaction.repository.AccountMovementRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class AccountStatementServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 14, 17, 0);
    private static final UUID ACCOUNT_ID = UUID.fromString("f1f2f3f4-1111-4222-8333-444455556666");
    private static final UUID CUSTOMER_ID = UUID.fromString("22222222-3333-4444-5555-666677778888");

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMovementRepository movementRepository;

    private AccountStatementService service;

    @BeforeEach
    void setUp() {
        service = new AccountStatementService(
                accountRepository,
                movementRepository,
                Clock.fixed(Instant.parse("2026-06-14T17:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void returnsStatementWithMovementsAndKeepsBalancesUnchanged() {
        AccountEntity account = account("5300000001", "250.00");
        AccountMovementEntity movement1 = movement(MovementType.CREDIT, "50.00", "Payroll deposit");
        AccountMovementEntity movement2 = movement(MovementType.DEBIT, "20.00", "ATM withdrawal");
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));

        when(accountRepository.findByAccountNumber("5300000001"))
                .thenReturn(Optional.of(account));
        when(movementRepository.findStatementMovements(
                ACCOUNT_ID,
                null,
                null,
                pageRequest))
                .thenReturn(new PageImpl<>(new ArrayList<>(List.of(movement1, movement2)), pageRequest, 2));

        AccountStatementResponse response = service.getStatement(
                "5300000001", null, null, pageRequest);

        assertThat(response.accountNumber()).isEqualTo("5300000001");
        assertThat(response.balance()).isEqualByComparingTo("250.00");
        assertThat(response.availableBalance()).isEqualByComparingTo("250.00");
        assertThat(account.getBalance()).isEqualByComparingTo("250.00");
        assertThat(response.movements()).hasSize(2);
        assertThat(response.movements().get(0).movementType()).isEqualTo(MovementType.CREDIT);
        assertThat(response.movements().get(0).description()).isEqualTo("Payroll deposit");
        assertThat(response.page().totalElements()).isEqualTo(2);
        assertThat(response.statementDate()).isEqualTo(NOW);
    }

    @Test
    void returnsEmptyMovementsForExistingAccountWithoutHistory() {
        AccountEntity account = account("5300000001", "250.00");
        PageRequest pageRequest = PageRequest.of(0, 10);

        when(accountRepository.findByAccountNumber("5300000001"))
                .thenReturn(Optional.of(account));
        when(movementRepository.findStatementMovements(
                ACCOUNT_ID,
                null,
                null,
                pageRequest))
                .thenReturn(new PageImpl<>(new ArrayList<>(), pageRequest, 0));

        AccountStatementResponse response = service.getStatement(
                "5300000001", null, null, pageRequest);

        assertThat(response.movements()).isEmpty();
        assertThat(response.page().totalElements()).isZero();
        assertThat(response.page().last()).isTrue();
    }

    @Test
    void rejectsMissingAccount() {
        when(accountRepository.findByAccountNumber("5300000001"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStatement(
                "5300000001", null, null, PageRequest.of(0, 10)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("No se encontró la cuenta solicitada.");
    }

    @Test
    void rejectsInvalidAccountNumberAndDateRange() {
        assertThatThrownBy(() -> service.getStatement(
                "ABC", null, null, PageRequest.of(0, 10)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("El número de cuenta debe contener exactamente 10 dígitos.");

        assertThatThrownBy(() -> service.getStatement(
                "5300000001",
                LocalDateTime.of(2026, 6, 14, 10, 0),
                LocalDateTime.of(2026, 6, 14, 9, 0),
                PageRequest.of(0, 10)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("La fecha inicial debe ser anterior o igual a la fecha final.");
    }

    @Test
    void passesPaginationAndSortToRepository() {
        AccountEntity account = account("5300000001", "250.00");
        PageRequest pageRequest = PageRequest.of(2, 5, Sort.by(Sort.Direction.ASC, "amount"));
        when(accountRepository.findByAccountNumber("5300000001"))
                .thenReturn(Optional.of(account));
        when(movementRepository.findStatementMovements(
                ACCOUNT_ID,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 12, 31, 23, 59),
                pageRequest))
                .thenReturn(new PageImpl<>(new ArrayList<>(), pageRequest, 0));

        service.getStatement(
                "5300000001",
                LocalDateTime.of(2026, 1, 1, 0, 0),
                LocalDateTime.of(2026, 12, 31, 23, 59),
                pageRequest);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(movementRepository).findStatementMovements(
                any(UUID.class),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(5);
        assertThat(captor.getValue().getSort().getOrderFor("amount")).isNotNull();
    }

    private AccountEntity account(String number, String balance) {
        BigDecimal amount = new BigDecimal(balance);
        return new AccountEntity(
                ACCOUNT_ID,
                number,
                AccountType.SAVINGS,
                AccountStatus.ACTIVE,
                amount,
                amount,
                false,
                customer(),
                NOW,
                NOW);
    }

    private AccountMovementEntity movement(MovementType type, String amount, String description) {
        FinancialTransactionEntity transaction = new FinancialTransactionEntity(
                UUID.randomUUID(),
                TransactionType.DEPOSIT,
                new BigDecimal(amount),
                null,
                account("5300000001", "250.00"),
                TransactionStatus.SUCCESS,
                description,
                NOW,
                NOW);
        return new AccountMovementEntity(
                UUID.randomUUID(),
                account("5300000001", "250.00"),
                transaction,
                type,
                new BigDecimal(amount),
                new BigDecimal("250.00"),
                NOW);
    }

    private CustomerEntity customer() {
        return new CustomerEntity(
                CUSTOMER_ID,
                "CC",
                "900001",
                "Carlos",
                "Villamil",
                "customer@example.com",
                null,
                LocalDate.of(1990, 1, 1),
                NOW,
                NOW);
    }
}