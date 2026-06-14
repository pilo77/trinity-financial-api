package com.trinity.financial.transaction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.trinity.financial.account.entity.AccountEntity;
import com.trinity.financial.account.entity.AccountStatus;
import com.trinity.financial.account.entity.AccountType;
import com.trinity.financial.account.repository.AccountRepository;
import com.trinity.financial.customer.entity.CustomerEntity;
import com.trinity.financial.shared.exception.BusinessRuleException;
import com.trinity.financial.shared.exception.InvalidRequestException;
import com.trinity.financial.shared.exception.ResourceNotFoundException;
import com.trinity.financial.transaction.dto.DepositRequest;
import com.trinity.financial.transaction.dto.TransactionMapper;
import com.trinity.financial.transaction.dto.TransactionResponse;
import com.trinity.financial.transaction.dto.TransferRequest;
import com.trinity.financial.transaction.dto.WithdrawalRequest;
import com.trinity.financial.transaction.entity.AccountMovementEntity;
import com.trinity.financial.transaction.entity.FinancialTransactionEntity;
import com.trinity.financial.transaction.entity.MovementType;
import com.trinity.financial.transaction.repository.AccountMovementRepository;
import com.trinity.financial.transaction.repository.FinancialTransactionRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 6, 14, 17, 0);

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private FinancialTransactionRepository transactionRepository;
    @Mock
    private AccountMovementRepository movementRepository;

    private TransactionService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-14T17:00:00Z"), ZoneOffset.UTC);
        service = new TransactionService(
                accountRepository,
                transactionRepository,
                movementRepository,
                new TransactionMapper(),
                clock);
        lenient().when(transactionRepository.saveAndFlush(any(FinancialTransactionEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void depositsAndCreatesCreditMovementWithNullSource() {
        AccountEntity account = account("5300000001", AccountStatus.ACTIVE, "100.00");
        when(accountRepository.findByAccountNumberForUpdate("5300000001"))
                .thenReturn(Optional.of(account));
        when(movementRepository.saveAndFlush(any(AccountMovementEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response =
                service.deposit(new DepositRequest("5300000001", amount("25.00"), " test "));

        assertThat(account.getBalance()).isEqualByComparingTo("125.00");
        assertThat(account.getAvailableBalance()).isEqualByComparingTo("125.00");
        assertThat(response.sourceAccountNumber()).isNull();
        assertThat(response.destinationAccountNumber()).isEqualTo("5300000001");
        assertThat(response.movements()).singleElement()
                .extracting(movement -> movement.movementType())
                .isEqualTo(MovementType.CREDIT);
        assertThat(response.description()).isEqualTo("test");
    }

    @Test
    void withdrawsAndCreatesDebitMovementWithNullDestination() {
        AccountEntity account = account("5300000001", AccountStatus.ACTIVE, "100.00");
        when(accountRepository.findByAccountNumberForUpdate("5300000001"))
                .thenReturn(Optional.of(account));
        when(movementRepository.saveAndFlush(any(AccountMovementEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response =
                service.withdraw(new WithdrawalRequest("5300000001", amount("40.00"), null));

        assertThat(account.getBalance()).isEqualByComparingTo("60.00");
        assertThat(account.getAvailableBalance()).isEqualByComparingTo("60.00");
        assertThat(response.sourceAccountNumber()).isEqualTo("5300000001");
        assertThat(response.destinationAccountNumber()).isNull();
        assertThat(response.movements()).singleElement()
                .extracting(movement -> movement.movementType())
                .isEqualTo(MovementType.DEBIT);
    }

    @Test
    void rejectsWithdrawalWithoutFunds() {
        AccountEntity account = account("5300000001", AccountStatus.ACTIVE, "10.00");
        when(accountRepository.findByAccountNumberForUpdate("5300000001"))
                .thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.withdraw(
                new WithdrawalRequest("5300000001", amount("10.01"), null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La cuenta no tiene fondos suficientes.");
        verify(transactionRepository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsZeroNegativeAndExcessScaleAmounts() {
        assertThatThrownBy(() -> service.deposit(
                new DepositRequest("5300000001", BigDecimal.ZERO, null)))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.deposit(
                new DepositRequest("5300000001", amount("-1.00"), null)))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.deposit(
                new DepositRequest("5300000001", amount("1.001"), null)))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void rejectsUnknownInactiveAndCancelledAccounts() {
        when(accountRepository.findByAccountNumberForUpdate("5300000001"))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deposit(
                new DepositRequest("5300000001", amount("1.00"), null)))
                .isInstanceOf(ResourceNotFoundException.class);

        AccountEntity inactive = account("3300000001", AccountStatus.INACTIVE, "10.00");
        when(accountRepository.findByAccountNumberForUpdate("3300000001"))
                .thenReturn(Optional.of(inactive));
        assertThatThrownBy(() -> service.deposit(
                new DepositRequest("3300000001", amount("1.00"), null)))
                .isInstanceOf(BusinessRuleException.class);

        AccountEntity cancelled = account("3300000002", AccountStatus.CANCELLED, "10.00");
        when(accountRepository.findByAccountNumberForUpdate("3300000002"))
                .thenReturn(Optional.of(cancelled));
        assertThatThrownBy(() -> service.deposit(
                new DepositRequest("3300000002", amount("1.00"), null)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void transfersWithLexicographicLockOrderAndTwoMovements() {
        AccountEntity source = account("5300000002", AccountStatus.ACTIVE, "100.00");
        AccountEntity destination = account("3300000001", AccountStatus.ACTIVE, "20.00");
        when(accountRepository.findByAccountNumberForUpdate("3300000001"))
                .thenReturn(Optional.of(destination));
        when(accountRepository.findByAccountNumberForUpdate("5300000002"))
                .thenReturn(Optional.of(source));
        when(movementRepository.saveAllAndFlush(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransactionResponse response = service.transfer(
                new TransferRequest("5300000002", "3300000001", amount("30.00"), null));

        assertThat(source.getBalance()).isEqualByComparingTo("70.00");
        assertThat(destination.getBalance()).isEqualByComparingTo("50.00");
        assertThat(response.movements())
                .extracting(movement -> movement.movementType())
                .containsExactly(MovementType.DEBIT, MovementType.CREDIT);
        var inOrder = org.mockito.Mockito.inOrder(accountRepository);
        inOrder.verify(accountRepository).findByAccountNumberForUpdate("3300000001");
        inOrder.verify(accountRepository).findByAccountNumberForUpdate("5300000002");
    }

    @Test
    void rejectsSameAccountTransferInsufficientFundsAndMissingDestination() {
        assertThatThrownBy(() -> service.transfer(
                new TransferRequest("5300000001", "5300000001", amount("1.00"), null)))
                .isInstanceOf(InvalidRequestException.class);

        AccountEntity source = account("3300000001", AccountStatus.ACTIVE, "1.00");
        AccountEntity destination = account("5300000001", AccountStatus.ACTIVE, "10.00");
        when(accountRepository.findByAccountNumberForUpdate("3300000001"))
                .thenReturn(Optional.of(source));
        when(accountRepository.findByAccountNumberForUpdate("5300000001"))
                .thenReturn(Optional.of(destination));
        assertThatThrownBy(() -> service.transfer(
                new TransferRequest("3300000001", "5300000001", amount("2.00"), null)))
                .isInstanceOf(BusinessRuleException.class);

        when(accountRepository.findByAccountNumberForUpdate("5300000002"))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.transfer(
                new TransferRequest("3300000001", "5300000002", amount("1.00"), null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findsTransactionWithItsMovements() {
        UUID id = UUID.randomUUID();
        AccountEntity account = account("5300000001", AccountStatus.ACTIVE, "100.00");
        FinancialTransactionEntity transaction = new FinancialTransactionEntity(
                id, com.trinity.financial.transaction.entity.TransactionType.DEPOSIT,
                amount("10.00"), null, account,
                com.trinity.financial.transaction.entity.TransactionStatus.SUCCESS,
                null, NOW, NOW);
        AccountMovementEntity movement = new AccountMovementEntity(
                UUID.randomUUID(), account, transaction, MovementType.CREDIT,
                amount("10.00"), amount("100.00"), NOW);
        when(transactionRepository.findById(id)).thenReturn(Optional.of(transaction));
        when(movementRepository.findByFinancialTransactionIdOrderByCreatedAtAscIdAsc(id))
                .thenReturn(List.of(movement));

        assertThat(service.findById(id).movements()).hasSize(1);
    }

    private AccountEntity account(String number, AccountStatus status, String balance) {
        return new AccountEntity(
                UUID.randomUUID(), number,
                number.startsWith("53") ? AccountType.SAVINGS : AccountType.CHECKING,
                status, amount(balance), amount(balance), false, customer(), NOW, NOW);
    }

    private CustomerEntity customer() {
        return new CustomerEntity(
                UUID.randomUUID(), "CC", UUID.randomUUID().toString(),
                "Carlos", "Villamil", UUID.randomUUID() + "@example.com", null,
                LocalDate.of(1990, 1, 1), NOW, NOW);
    }

    private BigDecimal amount(String value) {
        return new BigDecimal(value);
    }
}
