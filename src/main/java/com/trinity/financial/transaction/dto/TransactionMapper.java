package com.trinity.financial.transaction.dto;

import com.trinity.financial.transaction.entity.AccountMovementEntity;
import com.trinity.financial.transaction.entity.FinancialTransactionEntity;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionResponse toResponse(
            FinancialTransactionEntity transaction,
            List<AccountMovementEntity> movements) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getTransactionType(),
                transaction.getAmount(),
                transaction.getSourceAccount() == null
                        ? null
                        : transaction.getSourceAccount().getAccountNumber(),
                transaction.getDestinationAccount() == null
                        ? null
                        : transaction.getDestinationAccount().getAccountNumber(),
                transaction.getStatus(),
                transaction.getDescription(),
                transaction.getTransactionDate(),
                transaction.getCreatedAt(),
                movements.stream().map(this::toMovementResponse).toList());
    }

    private AccountMovementResponse toMovementResponse(AccountMovementEntity movement) {
        return new AccountMovementResponse(
                movement.getId(),
                movement.getAccount().getAccountNumber(),
                movement.getMovementType(),
                movement.getAmount(),
                movement.getBalanceAfterMovement(),
                movement.getCreatedAt());
    }
}
