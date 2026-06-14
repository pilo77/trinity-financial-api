package com.trinity.financial.transaction.dto;

import com.trinity.financial.transaction.entity.TransactionStatus;
import com.trinity.financial.transaction.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TransactionResponse(
        UUID id,
        TransactionType transactionType,
        BigDecimal amount,
        String sourceAccountNumber,
        String destinationAccountNumber,
        TransactionStatus status,
        String description,
        LocalDateTime transactionDate,
        LocalDateTime createdAt,
        List<AccountMovementResponse> movements) {
}
