package com.trinity.financial.transaction.service;

import com.trinity.financial.account.entity.AccountEntity;
import com.trinity.financial.account.entity.AccountStatus;
import com.trinity.financial.account.repository.AccountRepository;
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
import com.trinity.financial.transaction.entity.TransactionStatus;
import com.trinity.financial.transaction.entity.TransactionType;
import com.trinity.financial.transaction.repository.AccountMovementRepository;
import com.trinity.financial.transaction.repository.FinancialTransactionRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final FinancialTransactionRepository transactionRepository;
    private final AccountMovementRepository movementRepository;
    private final TransactionMapper transactionMapper;
    private final Clock clock;

    public TransactionService(
            AccountRepository accountRepository,
            FinancialTransactionRepository transactionRepository,
            AccountMovementRepository movementRepository,
            TransactionMapper transactionMapper,
            Clock clock) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.movementRepository = movementRepository;
        this.transactionMapper = transactionMapper;
        this.clock = clock;
    }

    @Transactional
    public TransactionResponse deposit(DepositRequest request) {
        validateAmount(request.amount());
        AccountEntity account = findLockedAccount(request.accountNumber());
        validateActive(account);
        LocalDateTime now = LocalDateTime.now(clock);
        account.credit(request.amount(), now);

        FinancialTransactionEntity transaction = saveTransaction(
                TransactionType.DEPOSIT,
                request.amount(),
                null,
                account,
                request.description(),
                now);
        AccountMovementEntity movement = movement(
                account,
                transaction,
                MovementType.CREDIT,
                request.amount(),
                now);
        movementRepository.saveAndFlush(movement);
        return transactionMapper.toResponse(transaction, List.of(movement));
    }

    @Transactional
    public TransactionResponse withdraw(WithdrawalRequest request) {
        validateAmount(request.amount());
        AccountEntity account = findLockedAccount(request.accountNumber());
        validateActive(account);
        validateFunds(account, request.amount());
        LocalDateTime now = LocalDateTime.now(clock);
        account.debit(request.amount(), now);

        FinancialTransactionEntity transaction = saveTransaction(
                TransactionType.WITHDRAWAL,
                request.amount(),
                account,
                null,
                request.description(),
                now);
        AccountMovementEntity movement = movement(
                account,
                transaction,
                MovementType.DEBIT,
                request.amount(),
                now);
        movementRepository.saveAndFlush(movement);
        return transactionMapper.toResponse(transaction, List.of(movement));
    }

    @Transactional
    public TransactionResponse transfer(TransferRequest request) {
        validateAmount(request.amount());
        if (request.sourceAccountNumber().equals(request.destinationAccountNumber())) {
            throw new InvalidRequestException(
                    "SAME_TRANSFER_ACCOUNT",
                    "Las cuentas origen y destino deben ser diferentes.");
        }

        String firstNumber = request.sourceAccountNumber()
                .compareTo(request.destinationAccountNumber()) < 0
                ? request.sourceAccountNumber()
                : request.destinationAccountNumber();
        String secondNumber = firstNumber.equals(request.sourceAccountNumber())
                ? request.destinationAccountNumber()
                : request.sourceAccountNumber();

        AccountEntity first = findLockedAccount(firstNumber);
        AccountEntity second = findLockedAccount(secondNumber);
        AccountEntity source = first.getAccountNumber().equals(request.sourceAccountNumber())
                ? first : second;
        AccountEntity destination = source == first ? second : first;

        validateActive(source);
        validateActive(destination);
        validateFunds(source, request.amount());
        LocalDateTime now = LocalDateTime.now(clock);
        source.debit(request.amount(), now);
        destination.credit(request.amount(), now);

        FinancialTransactionEntity transaction = saveTransaction(
                TransactionType.TRANSFER,
                request.amount(),
                source,
                destination,
                request.description(),
                now);
        AccountMovementEntity debit = movement(
                source, transaction, MovementType.DEBIT, request.amount(), now);
        AccountMovementEntity credit = movement(
                destination, transaction, MovementType.CREDIT, request.amount(), now);
        List<AccountMovementEntity> movements =
                movementRepository.saveAllAndFlush(List.of(debit, credit));
        return transactionMapper.toResponse(transaction, movements);
    }

    @Transactional(readOnly = true)
    public TransactionResponse findById(UUID id) {
        FinancialTransactionEntity transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "TRANSACTION_NOT_FOUND",
                        "No se encontró la transacción solicitada."));
        List<AccountMovementEntity> movements =
                movementRepository.findByFinancialTransactionIdOrderByCreatedAtAscIdAsc(id);
        return transactionMapper.toResponse(transaction, movements);
    }

    private AccountEntity findLockedAccount(String accountNumber) {
        return accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ACCOUNT_NOT_FOUND",
                        "No se encontró la cuenta solicitada."));
    }

    private void validateActive(AccountEntity account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessRuleException(
                    "ACCOUNT_NOT_ACTIVE",
                    "La cuenta debe estar activa para realizar la operación.");
        }
    }

    private void validateFunds(AccountEntity account, BigDecimal amount) {
        if (account.getAvailableBalance().compareTo(amount) < 0
                || account.getBalance().compareTo(amount) < 0) {
            throw new BusinessRuleException(
                    "INSUFFICIENT_FUNDS",
                    "La cuenta no tiene fondos suficientes.");
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0
                || amount.scale() > 2 || amount.precision() - amount.scale() > 17) {
            throw new InvalidRequestException(
                    "INVALID_TRANSACTION_AMOUNT",
                    "El monto debe ser positivo y tener máximo dos decimales.");
        }
    }

    private FinancialTransactionEntity saveTransaction(
            TransactionType type,
            BigDecimal amount,
            AccountEntity source,
            AccountEntity destination,
            String description,
            LocalDateTime now) {
        FinancialTransactionEntity transaction = new FinancialTransactionEntity(
                UUID.randomUUID(),
                type,
                amount,
                source,
                destination,
                TransactionStatus.SUCCESS,
                normalizeDescription(description),
                now,
                now);
        return transactionRepository.saveAndFlush(transaction);
    }

    private AccountMovementEntity movement(
            AccountEntity account,
            FinancialTransactionEntity transaction,
            MovementType movementType,
            BigDecimal amount,
            LocalDateTime now) {
        return new AccountMovementEntity(
                UUID.randomUUID(),
                account,
                transaction,
                movementType,
                amount,
                account.getBalance(),
                now);
    }

    private String normalizeDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }
}
