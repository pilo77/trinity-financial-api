package com.trinity.financial.account.service;

import com.trinity.financial.account.dto.AccountStatementMovementResponse;
import com.trinity.financial.account.dto.AccountStatementResponse;
import com.trinity.financial.account.dto.PageInfo;
import com.trinity.financial.account.entity.AccountEntity;
import com.trinity.financial.account.repository.AccountRepository;
import com.trinity.financial.shared.exception.InvalidRequestException;
import com.trinity.financial.shared.exception.ResourceNotFoundException;
import com.trinity.financial.transaction.entity.AccountMovementEntity;
import com.trinity.financial.transaction.repository.AccountMovementRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountStatementService {

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "id",
            "movementType",
            "amount",
            "createdAt");

    private final AccountRepository accountRepository;
    private final AccountMovementRepository movementRepository;
    private final Clock clock;

    public AccountStatementService(
            AccountRepository accountRepository,
            AccountMovementRepository movementRepository,
            Clock clock) {
        this.accountRepository = accountRepository;
        this.movementRepository = movementRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public AccountStatementResponse getStatement(
            String accountNumber,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {
        validateAccountNumber(accountNumber);
        validateDateRange(startDate, endDate);
        validateSort(pageable);

        AccountEntity account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "ACCOUNT_NOT_FOUND",
                        "No se encontró la cuenta solicitada."));

        Page<AccountMovementEntity> page = findMovements(
                account.getId(), startDate, endDate, pageable);

        List<AccountStatementMovementResponse> movements = page.getContent().stream()
                .map(this::toMovementResponse)
                .toList();

        return new AccountStatementResponse(
                account.getAccountNumber(),
                account.getAccountType(),
                account.getStatus(),
                account.getBalance(),
                account.getAvailableBalance(),
                LocalDateTime.now(clock),
                movements,
                new PageInfo(
                        page.getNumber(),
                        page.getSize(),
                        page.getTotalElements(),
                        page.getTotalPages(),
                        page.isFirst(),
                        page.isLast(),
                        page.hasNext(),
                        page.hasPrevious()));
    }

    private void validateAccountNumber(String accountNumber) {
        if (accountNumber == null || !accountNumber.matches("\\d{10}")) {
            throw new InvalidRequestException(
                    "INVALID_ACCOUNT_NUMBER",
                    "El número de cuenta debe contener exactamente 10 dígitos.");
        }
    }

    private Page<AccountMovementEntity> findMovements(
            UUID accountId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable) {
        if (startDate != null && endDate != null) {
            return movementRepository.findByAccountIdAndCreatedAtBetween(
                    accountId, startDate, endDate, pageable);
        }
        if (startDate != null) {
            return movementRepository.findByAccountIdAndCreatedAtGreaterThanEqual(
                    accountId, startDate, pageable);
        }
        if (endDate != null) {
            return movementRepository.findByAccountIdAndCreatedAtLessThanEqual(
                    accountId, endDate, pageable);
        }
        return movementRepository.findByAccountId(accountId, pageable);
    }

    private void validateDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new InvalidRequestException(
                    "INVALID_DATE_RANGE",
                    "La fecha inicial debe ser anterior o igual a la fecha final.");
        }
    }

    private void validateSort(Pageable pageable) {
        boolean invalidSort = pageable.getSort().stream()
                .map(order -> order.getProperty())
                .anyMatch(property -> !ALLOWED_SORT_PROPERTIES.contains(property));
        if (invalidSort) {
            throw new InvalidRequestException(
                    "INVALID_STATEMENT_SORT",
                    "El criterio de ordenamiento del estado de cuenta no es válido.");
        }
    }

    private AccountStatementMovementResponse toMovementResponse(AccountMovementEntity movement) {
        return new AccountStatementMovementResponse(
                movement.getId(),
                movement.getMovementType(),
                movement.getAmount(),
                movement.getFinancialTransaction().getId(),
                movement.getCreatedAt(),
                movement.getFinancialTransaction().getDescription());
    }
}
